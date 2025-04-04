package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.os.Build
import androidx.fragment.app.FragmentActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.InstallationUtils
import dev.brahmkshatriya.echo.extensions.InstallationUtils.getTempApkDir
import dev.brahmkshatriya.echo.utils.ContextUtils.appVersion
import dev.brahmkshatriya.echo.utils.Serializer.toData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.use
import org.koin.android.ext.android.inject
import java.io.File

object AppUpdater {
    val client = OkHttpClient()
    suspend fun FragmentActivity.updateApp() {
        val app by inject<App>()
        val throwableFlow = app.throwFlow
        val messageFlow = app.messageFlow
        val currentVersion = appVersion()
        val updateUrl = "https://api.github.com/repos/brahmkshatriya/echo/releases"

        val url = runCatching {
            getGithubUpdateUrl(currentVersion, updateUrl, client) ?: return
        }.getOrElse {
            throwableFlow.emit(it)
            return
        }

        messageFlow.emit(
            Message(
                getString(R.string.downloading_update_for_x, getString(R.string.app_name))
            )
        )
        val file = runCatching {
            downloadUpdate(this, url, client)
        }.getOrElse {
            throwableFlow.emit(it)
            return
        }
        InstallationUtils.installExtension(this, file, true).getOrElse {
            throwableFlow.emit(it)
        }
    }

    private val githubRegex = Regex("https://api\\.github\\.com/repos/([^/]*)/([^/]*)/")
    suspend fun getGithubUpdateUrl(
        currentVersion: String,
        updateUrl: String,
        client: OkHttpClient
    ) = run {
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
                it.name.endsWith("apk")
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

    suspend fun downloadUpdate(
        context: Context,
        url: String,
        client: OkHttpClient
    ) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val res = client.newCall(request).await().body.byteStream()
        val file = File.createTempFile("temp", ".apk", context.getTempApkDir())
        res.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        file
    }
}