package dev.brahmkshatriya.echo.builtin.test

import android.content.Context
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.helpers.FileProgress
import dev.brahmkshatriya.echo.common.helpers.FileTask
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.helpers.Progress
import dev.brahmkshatriya.echo.common.helpers.SuspendedFunction
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.providers.MusicExtensionsProvider
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extensions.get
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.File

class DownloadExtension(
    val context: Context
) : DownloadClient, MusicExtensionsProvider {

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

    override val concurrentDownloads = 2

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
    override suspend fun getDownloadTracks(
        extensionId: String,
        item: EchoMediaItem
    ): List<DownloadContext> {
        return when (item) {
            is EchoMediaItem.TrackItem -> listOf(DownloadContext(extensionId, item.track))
            is EchoMediaItem.Lists -> {
                val ext = exts.first { it.id == extensionId }
                val tracks = when (item) {
                    is EchoMediaItem.Lists.AlbumItem -> ext.get<AlbumClient, List<Track>> {
                        val album = loadAlbum(item.album)
                        val tracks = loadTracks(album).loadAll()
                        tracks
                    }

                    is EchoMediaItem.Lists.PlaylistItem -> ext.get<PlaylistClient, List<Track>> {
                        val album = loadPlaylist(item.playlist)
                        val tracks = loadTracks(album).loadAll()
                        tracks
                    }

                    is EchoMediaItem.Lists.RadioItem -> ext.get<RadioClient, List<Track>> {
                        loadTracks(item.radio).loadAll()
                    }
                }.getOrThrow()
                tracks.mapIndexed { index, track ->
                    DownloadContext(extensionId, track, index, item)
                }
            }

            else -> listOf()
        }
    }

    class TestTask(
        val file: File, val name: String
    ) : FileTask {
        override val progressFlow = MutableSharedFlow<FileProgress>()
        private var job: Job? = null
        override val start = SuspendedFunction {
            job = launch {
                progressFlow.emit(Progress.Initialized(100))
                var it = 0
                while(it < 10) {
                    Thread.sleep(1000)
                    progressFlow.emit(Progress.InProgress(it * 10L, null))
                    it++
                }
                progressFlow.emit(Progress.Final.Completed(0L, file))
            }
        }

        override val cancel = SuspendedFunction {
            job?.cancel()
            progressFlow.emit(Progress.Final.Cancelled())
        }
        override val pause = null
        override val resume = null
    }

    override suspend fun onExtensionSelected() {}
    override val settingItems: List<Setting> = listOf()
    override fun setSettings(settings: Settings) {}
    override val requiredMusicExtensions = listOf<String>()

    private lateinit var exts: List<MusicExtension>
    override fun setMusicExtensions(extensions: List<MusicExtension>) {
        exts = extensions
    }
}