package dev.brahmkshatriya.echo.extensions

import android.content.Context
import android.os.Build
import androidx.fragment.app.FragmentActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.await
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.with
import dev.brahmkshatriya.echo.extensions.InstallationUtils.cleanupTempApks
import dev.brahmkshatriya.echo.extensions.InstallationUtils.getTempApkDir
import dev.brahmkshatriya.echo.extensions.exceptions.InvalidExtensionListException
import dev.brahmkshatriya.echo.extensions.exceptions.UpdateException
import dev.brahmkshatriya.echo.utils.CacheUtils.getFromCache
import dev.brahmkshatriya.echo.utils.CacheUtils.saveToCache
import dev.brahmkshatriya.echo.utils.Serializer.toData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.use
import java.io.File

class Updater(
    extensionLoader: ExtensionLoader
) {
    private val app = extensionLoader.app
    private val settings = app.settings
    private val throwableFlow = app.throwFlow
    private val messageFlow = app.messageFlow

    val client = OkHttpClient()
    private val extensions = extensionLoader.extensions

    private val updateTime = 1000 * 60 * 60 * 6
    private fun Context.shouldCheckForUpdates(): Boolean {
        val check = settings.getBoolean("check_for_extension_updates", true)
        if (!check) return false
        val lastUpdateCheck = getFromCache<Long>("last_update_check") ?: 0
        return System.currentTimeMillis() - lastUpdateCheck > updateTime
    }

    suspend fun updateExtensions(activity: FragmentActivity, force: Boolean) {
        if (!activity.shouldCheckForUpdates() && !force) return
        activity.saveToCache("last_update_check", System.currentTimeMillis())
        activity.cleanupTempApks()
        messageFlow.emit(Message(activity.getString(R.string.checking_for_extension_updates)))
        extensions.all.await().forEach {
            updateExtension(activity, it)
        }
    }

    private suspend fun updateExtension(
        activity: FragmentActivity,
        extension: Extension<*>
    ) {
        val currentVersion = extension.version
        val updateUrl = extension.metadata.updateUrl ?: return

        val url = extension.with(throwableFlow) {
            getUpdateFileUrl(currentVersion, updateUrl, client).getOrThrow()
        } ?: return

        messageFlow.emit(
            Message(
                activity.getString(R.string.downloading_update_for_extension, extension.name)
            )
        )
        val file = extension.with(throwableFlow) {
            downloadUpdate(activity, url, client).getOrThrow()
        } ?: return
        val installAsApk = extension.metadata.importType == ImportType.App
        val successful =
            InstallationUtils.installExtension(activity, file, installAsApk).getOrElse {
                throwableFlow.emit(it)
                false
            }
        if (successful) messageFlow.emit(
            Message(
                activity.getString(R.string.extension_updated_successfully, extension.name)
            )
        )
    }

    private suspend fun <T> runIOCatching(
        block: suspend () -> T
    ) = withContext(Dispatchers.IO) {
        runCatching {
            block()
        }.getOrElse {
            return@withContext Result.failure<T>(UpdateException(it))
        }.let { Result.success(it) }
    }

    suspend fun downloadUpdate(
        context: Context,
        url: String,
        client: OkHttpClient
    ) = runIOCatching {
        val request = Request.Builder().url(url).build()
        val res = client.newCall(request).await().body.byteStream()
        val file = File.createTempFile("temp", ".apk", context.getTempApkDir())
        res.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        file
    }

    suspend fun getUpdateFileUrl(
        currentVersion: String,
        updateUrl: String,
        client: OkHttpClient
    ) = runIOCatching {
        if (updateUrl.isEmpty()) return@runIOCatching null
        if (updateUrl.startsWith("https://api.github.com/repos/")) {
            getGithubUpdateUrl(currentVersion, updateUrl, client).getOrThrow()
        } else {
            throw Exception("Unsupported update url")
        }
    }


    private val githubRegex = Regex("https://api\\.github\\.com/repos/([^/]*)/([^/]*)/")
    private suspend fun getGithubUpdateUrl(
        currentVersion: String,
        updateUrl: String,
        client: OkHttpClient
    ) = runIOCatching {
        val (user, repo) = githubRegex.find(updateUrl)?.destructured
            ?: throw Exception("Invalid Github URL")
        val url = "https://api.github.com/repos/$user/$repo/releases/latest"
        val request = Request.Builder().url(url).build()
        val res = runCatching {
            client.newCall(request).await().use {
                it.body.string().toData<GithubResponse>()
            }
        }.getOrElse {
            throw Exception("Failed to fetch latest release", it)
        }
        if (res.tagName != currentVersion) {
            res.assets.sortedByDescending {
                it.name.contains(Build.SUPPORTED_ABIS.first())
            }.firstOrNull {
                it.name.endsWith(".eapk")
            }?.browserDownloadUrl ?: throw Exception("No EApk assets found")
        } else {
            null
        }
    }

    @Serializable
    data class GithubResponse(
        @SerialName("tag_name")
        val tagName: String,
        @SerialName("created_at")
        val createdAt: String,
        val assets: List<Asset>
    )

    @Serializable
    data class Asset(
        val name: String,
        @SerialName("browser_download_url")
        val browserDownloadUrl: String
    )

    suspend fun getExtensionList(
        link: String,
        client: OkHttpClient
    ) = runIOCatching {
        val request = Request.Builder()
            .addHeader("Cookie", "preview=1")
            .url(link).build()
        client.newCall(request).await().body.string().toData<List<ExtensionAssetResponse>>()
    }.getOrElse {
        throw InvalidExtensionListException(link, it)
    }

    @Serializable
    data class ExtensionAssetResponse(
        val id: String,
        val name: String,
        val subtitle: String? = null,
        val iconUrl: String? = null,
        val updateUrl: String
    )

    sealed class AddState {
        data object Init : AddState()
        data object Loading : AddState()
        data class AddList(val list: List<ExtensionAssetResponse>?) : AddState()
    }

    val addingFlow = MutableStateFlow<AddState>(AddState.Init)
    suspend fun addFromLinkOrCode(link: String) {
        addingFlow.value = AddState.Loading
        val actualLink = when {
            link.startsWith("http://") or link.startsWith("https://") -> link
            else -> "https://v.gd/$link"
        }

        val list = runCatching { getExtensionList(actualLink, client) }.getOrElse {
            app.throwFlow.emit(it)
            null
        }
        addingFlow.value = AddState.AddList(list)
    }
}