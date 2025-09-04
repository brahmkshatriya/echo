package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.os.Build
import dev.brahmkshatriya.echo.BuildConfig
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.utils.ContextUtils.appVersion
import dev.brahmkshatriya.echo.utils.ContextUtils.getTempFile
import dev.brahmkshatriya.echo.utils.Serializer.toData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.use
import java.io.File
import java.util.zip.ZipFile

object AppUpdater {

    private val client = OkHttpClient()

    @Suppress("KotlinConstantConditions")
    suspend fun updateApp(app: App): File? {
        val throwableFlow = app.throwFlow
        val messageFlow = app.messageFlow
        val githubRepo = app.context.getString(R.string.app_github_repo)
        val appType = BuildConfig.BUILD_TYPE
        val version = appVersion()

        val url = runCatching {
            when (appType) {
                "stable" -> {
                    val currentVersion = version.substringBefore('_')
                    val updateUrl = "https://api.github.com/repos/$githubRepo/releases"
                    getGithubUpdateUrl(currentVersion, updateUrl, client) ?: return null
                }

                "nightly" -> {
                    val hash = version.substringBefore("(").substringAfter('_')
                    val id = getGithubWorkflowId(hash, githubRepo, client) ?: return null
                    "https://nightly.link/$githubRepo/actions/runs/$id/artifact.zip"
                }

                else -> return null
            }
        }.getOrElse {
            throwableFlow.emit(it)
            return null
        }

        messageFlow.emit(
            Message(
                app.context.run {
                    getString(R.string.downloading_update_for_x, getString(R.string.app_name))
                }
            )
        )
        return runCatching {
            val download = downloadUpdate(app.context, url, client).getOrThrow()
            if (appType == "stable") download else unzipApk(download)
        }.getOrElse {
            throwableFlow.emit(it)
            return null
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
                it.body.string().toData<GithubReleaseResponse>()
            }.getOrThrow()
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

    private suspend fun getGithubWorkflowId(
        hash: String,
        githubRepo: String,
        client: OkHttpClient
    ) = runCatching {
        val url =
            "https://api.github.com/repos/$githubRepo/actions/workflows/nightly.yml/runs?per_page=1&conclusion=success"
        val request = Request.Builder().url(url).build()
        client.newCall(request).await().body.string().toData<GithubRunsResponse>().getOrThrow()
            .workflowRuns.firstOrNull { it.sha.take(7) != hash }?.id
    }.getOrElse {
        throw Exception("Failed to fetch workflow ID", it)
    }

    @Serializable
    data class GithubReleaseResponse(
        @SerialName("tag_name")
        val tagName: String,
        @SerialName("created_at")
        val createdAt: String,
        val assets: List<Asset>
    ) {
        @Serializable
        data class Asset(
            val name: String,
            @SerialName("browser_download_url")
            val browserDownloadUrl: String
        )
    }

    @Serializable
    data class GithubRunsResponse(
        @SerialName("workflow_runs")
        val workflowRuns: List<Run>
    ) {
        @Serializable
        data class Run(
            val id: Long,
            @SerialName("head_sha")
            val sha: String,
        )
    }

    suspend fun downloadUpdate(
        context: Context,
        url: String,
        client: OkHttpClient
    ) = runIOCatching {
        val request = Request.Builder().url(url).build()
        val res = client.newCall(request).await().body.byteStream()
        val file = context.getTempFile()
        res.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        file
    }

    private fun unzipApk(file: File): File {
        val zipFile = ZipFile(file)
        val apkFile = File.createTempFile("temp", ".apk", file.parentFile!!)
        zipFile.use { zip ->
            val apkEntry = zip.entries().asSequence().firstOrNull {
                !it.isDirectory && it.name.endsWith(".apk")
            } ?: throw Exception("No APK file found in the zip")
            zip.getInputStream(apkEntry).use { input ->
                apkFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return apkFile
    }

    suspend fun getUpdateFileUrl(
        currentVersion: String,
        updateUrl: String,
        client: OkHttpClient
    ) = runIOCatching {
        if (updateUrl.isEmpty()) return@runIOCatching null
        if (updateUrl.startsWith("https://api.github.com/repos/")) {
            getGithubUpdateUrl(currentVersion, updateUrl, client)
        } else {
            throw Exception("Unsupported update url")
        }
    }

    private suspend fun <T> runIOCatching(
        block: suspend () -> T
    ) = withContext(Dispatchers.IO) {
        runCatching { runCatching { block() }.getOrElse { throw UpdateException(it) } }
    }

    class UpdateException(override val cause: Throwable) : Exception(cause) {
        override val message: String
            get() = "Update failed: ${cause.message}"
    }
}