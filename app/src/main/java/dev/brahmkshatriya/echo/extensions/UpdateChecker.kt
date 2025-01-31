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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
    if (updateUrl.startsWith("https://api.github.com")) {
        getGithubUpdateUrl(currentVersion, updateUrl, client).getOrThrow()
    } else {
        throw Exception("Unsupported update url")
    }
}


suspend fun getGithubUpdateUrl(
    currentVersion: String,
    updateUrl: String,
    client: OkHttpClient
) = runIOCatching {
    val request = Request.Builder().url(updateUrl).build()
    val res = client.newCall(request).await().use {
        it.body.string().toData<List<GithubResponse>>()
    }.maxByOrNull {
        dateFormat.parse(it.createdAt)?.time ?: 0
    } ?: return@runIOCatching null
    if (res.tagName != currentVersion) {
        res.assets.sortedBy {
            it.name.contains(Build.SUPPORTED_ABIS.first())
        }.firstOrNull {
            it.name.endsWith(".eapk")
        }?.browserDownloadUrl ?: throw Exception("No EApk assets found")
    } else {
        null
    }
}

val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.ENGLISH).apply {
    timeZone = TimeZone.getTimeZone("UTC")
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