package dev.brahmkshatriya.echo.extensions.builtin.test

import android.content.Context
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.Feed.Companion.loadAll
import dev.brahmkshatriya.echo.common.models.ImportType
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Progress
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.providers.MusicExtensionsProvider
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getAs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

class DownloadExtension(
    val context: Context
) : DownloadClient, MusicExtensionsProvider {

    companion object {
        val metadata = Metadata(
            "DownloadExtension",
            "",
            ImportType.BuiltIn,
            ExtensionType.MISC,
            "test_download",
            "Test Download Extension",
            "1.0.0",
            "Test extension for download testing",
            "Test",
        )
    }

    override val concurrentDownloads = 2

    override suspend fun selectServer(context: DownloadContext): Streamable {
        return context.track.servers.first()
    }

    override suspend fun selectSources(
        context: DownloadContext, server: Streamable.Media.Server
    ): List<Streamable.Source> {
        return server.sources
    }

    override suspend fun download(
        progressFlow: MutableStateFlow<Progress>,
        context: DownloadContext,
        source: Streamable.Source
    ) = test(progressFlow, "Downloading", 10000)

    override suspend fun merge(
        progressFlow: MutableStateFlow<Progress>,
        context: DownloadContext,
        files: List<File>
    ) = test(progressFlow, "Merging", 5000)

    override suspend fun tag(
        progressFlow: MutableStateFlow<Progress>,
        context: DownloadContext,
        file: File
    ) = test(progressFlow, "Tagging", 2000)

    override suspend fun getDownloadTracks(
        extensionId: String,
        item: EchoMediaItem,
        context: EchoMediaItem?
    ): List<DownloadContext> {
        return when (item) {
            is Track -> listOf(DownloadContext(extensionId, item))
            is EchoMediaItem.Lists -> {
                val ext = exts.first { it.id == extensionId }
                val tracks = when (item) {
                    is Album -> ext.getAs<AlbumClient, List<Track>> {
                        val album = loadAlbum(item)
                        val tracks = loadTracks(album)!!.loadAll()
                        tracks
                    }

                    is Playlist -> ext.getAs<PlaylistClient, List<Track>> {
                        val album = loadPlaylist(item)
                        val tracks = loadTracks(album).loadAll()
                        tracks
                    }

                    is Radio -> ext.getAs<RadioClient, List<Track>> {
                        val radio = loadRadio(item)
                        loadTracks(radio).loadAll()
                    }

                }.getOrThrow()
                tracks.mapIndexed { index, track ->
                    DownloadContext(extensionId, track, index, item)
                }
            }

            else -> listOf()
        }
    }

    private suspend fun test(
        progressFlow: MutableSharedFlow<Progress>,
        type: String,
        crash: Long
    ): File {
        progressFlow.emit(Progress(crash, 0))
        var it = 0L
        while (it < crash) {
            delay(1)
            progressFlow.emit(Progress(crash, it))
            it++
        }
        if (type == "Tagging") throw Exception("Test exception in $type")
        return this.context.cacheDir
    }

    override suspend fun getSettingItems() = listOf<Setting>()
    override fun setSettings(settings: Settings) {}
    override val requiredMusicExtensions = listOf<String>()

    private lateinit var exts: List<MusicExtension>
    override fun setMusicExtensions(extensions: List<MusicExtension>) {
        exts = extensions
    }
}