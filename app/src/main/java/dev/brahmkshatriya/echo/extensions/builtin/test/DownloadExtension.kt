package dev.brahmkshatriya.echo.extensions.builtin.test

import android.content.Context
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.Progress
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.providers.MusicExtensionsProvider
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
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

    override suspend fun onExtensionSelected() {}
    override val settingItems: List<Setting> = listOf()
    override fun setSettings(settings: Settings) {}
    override val requiredMusicExtensions = listOf<String>()

    private lateinit var exts: List<MusicExtension>
    override fun setMusicExtensions(extensions: List<MusicExtension>) {
        exts = extensions
    }
}