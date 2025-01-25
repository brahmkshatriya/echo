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
import kotlinx.coroutines.flow.MutableStateFlow
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
    ) = TestTask(file, "Download")

    override suspend fun merge(context: DownloadContext, files: List<File>, dir: File) =
        TestTask(dir, "Merge")

    override suspend fun tag(context: DownloadContext, file: File) = TestTask(file, "Tag")

    class TestTask(
        val file: File, val name: String
    ) : FileTask {
        private val command =
            "-f lavfi -i color=size=1280x720:rate=25:color=black -f lavfi -i anullsrc=channel_layout=stereo:sample_rate=44100 -t 10 \"${file.absolutePath}.mp4\""
        override val progressFlow = MutableStateFlow<FileProgress>(Progress.Initialized(null))
//        private var session: FFmpegSession? = null
        override val start = SuspendedFunction {
//            session = FFmpegKit.executeAsync(command, {
//                println("$name Completed: $it")
//                progressFlow.value = Progress.Final.Completed(0, file)
//            }, null, {
//                println("$name: $it")
//            })
        }

        override val cancel = SuspendedFunction {
//            session?.cancel()
            progressFlow.value = Progress.Final.Cancelled()
        }
        override val pause = null
        override val resume = null
    }

    override suspend fun onExtensionSelected() {}
    override val settingItems: List<Setting> = listOf()
    override fun setSettings(settings: Settings) {}
}