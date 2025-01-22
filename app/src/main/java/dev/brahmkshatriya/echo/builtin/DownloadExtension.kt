package dev.brahmkshatriya.echo.builtin

import android.content.Context
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.helpers.FileProgress
import dev.brahmkshatriya.echo.common.helpers.FileTask
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.helpers.Progress
import dev.brahmkshatriya.echo.common.helpers.SuspendedFunction
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

class DownloadExtension(
    val context: Context
) : DownloadClient {

    companion object {
        val metadata = Metadata(
            "DownloadExtension",
            "",
            ImportType.BuiltIn,
            "test_download",
            "Test Download Extension",
            "1.0.0",
            "Test extension for download testing",
            "Test",
        )
    }

    override val concurrentDownloads = 4

    override suspend fun getDownloadDir(context: DownloadContext): File {
        return this.context.cacheDir
    }

    override suspend fun selectServer(context: DownloadContext): Streamable {
        return context.track.servers.first()
    }

    override suspend fun selectSources(
        context: DownloadContext, server: Streamable.Media.Server
    ): List<Streamable.Source> {
        return server.sources
    }

    override suspend fun download(
        context: DownloadContext, source: Streamable.Source, file: File
    ) = TestTask(file)

    override suspend fun merge(context: DownloadContext, files: List<File>, dir: File) =
        TestTask(dir)

    override suspend fun tag(context: DownloadContext, file: File) = TestTask(file)

    class TestTask(
        val file: File
    ) : FileTask {
        override val progressFlow = MutableStateFlow<FileProgress>(Progress.Initialized(4))
        private var job: Job? = null
        override val start = SuspendedFunction {
            println("Starting Download")
            job?.cancel()
            job = coroutineScope {
                launch {
                    progressFlow.value = Progress.InProgress(0, 256)
                    for (i in 1L..4) {
                        delay(3000)
                        println("flow progress: from extension $i")
                        progressFlow.value = Progress.InProgress(i, 256)
                    }
                    progressFlow.value = Progress.Final.Completed(4, file)
                }
            }
        }

        override val cancel = SuspendedFunction {
            println("Cancelling Download")
            job?.cancel()
            progressFlow.value = Progress.Final.Cancelled()
        }
        override val pause = SuspendedFunction {
            println("Pausing Download")
            job?.cancel()
            progressFlow.value = Progress.Paused(4)
        }
        override val resume = SuspendedFunction {
            println("Resuming Download")
            start()
        }

    }

    override suspend fun onExtensionSelected() {}
    override val settingItems: List<Setting> = listOf()
    override fun setSettings(settings: Settings) {}
}