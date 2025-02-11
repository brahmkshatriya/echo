package dev.brahmkshatriya.echo.extensions

import android.content.Context
import android.os.Build
import dev.brahmkshatriya.echo.ExtensionOpenerActivity.Companion.getTempApkDir
import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.utils.toData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.use
import java.io.File

suspend fun <T> runIOCatching(
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


val githubRegex = Regex("https://api\\.github\\.com/repos/([^/]*)/([^/]*)/")
suspend fun getGithubUpdateUrl(
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
    throw InvalidExtensionListException(it)
}

@Serializable
data class ExtensionAssetResponse(
    val id: String,
    val name: String,
    val subtitle: String? = null,
    val iconUrl: String? = null,
    val updateUrl: String
)