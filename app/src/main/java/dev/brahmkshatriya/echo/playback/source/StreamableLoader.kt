package dev.brahmkshatriya.echo.playback.source

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Streamable.Source.Companion.toSource
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.MediaItemUtils.backgroundIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.downloaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.serverIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.subtitleIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.exceptions.NoServersException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

@UnstableApi
class StreamableLoader(
    private val context: Context,
    private val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val downloadFlow: StateFlow<List<Downloader.Info>>
) {
    suspend fun load(mediaItem: MediaItem) = withContext(Dispatchers.IO) {
        extensionListFlow.first { it != null }
        val new = if (mediaItem.isLoaded) mediaItem
        else MediaItemUtils.buildLoaded(
            context, downloadFlow.value, mediaItem, loadTrack(mediaItem)
        )

        val server = async { loadServer(new) }
        val background =
            async { if (new.backgroundIndex < 0) null else loadBackground(new).getOrNull() }
        val subtitle = async { if (new.subtitleIndex < 0) null else loadSubtitle(new).getOrNull() }

        MediaItemUtils.buildWithBackgroundAndSubtitle(
            new, background.await(), subtitle.await()
        ) to server.await()
    }

    private suspend fun <T> withClient(
        mediaItem: MediaItem,
        block: suspend TrackClient.() -> T
    ): Result<T> {
        val extension = extensionListFlow.getExtensionOrThrow(mediaItem.extensionId)
        return extension.get<TrackClient, T> { block() }
    }

    private suspend fun loadTrack(item: MediaItem): Track {
        val track = withClient(item) {
            loadTrack(item.track).also {
                it.servers.ifEmpty { throw NoServersException() }
            }
        }
        return track.getOrElse {
            downloadFlow.value.find { info ->
                info.download.trackId == item.track.id
            }?.download?.track ?: throw it
        }
    }

    private suspend fun loadServer(mediaItem: MediaItem): Streamable.Media.Server {
        val downloaded = mediaItem.downloaded
        val servers = mediaItem.track.servers
        val index = mediaItem.serverIndex
        if (!downloaded.isNullOrEmpty() && servers.size == index) {
            return Streamable.Media.Server(
                downloaded.map { Uri.fromFile(File(it)).toString().toSource() },
                true
            )
        }
        val streamable = servers[index]
        return withClient(mediaItem) {
            loadStreamableMedia(streamable, false) as Streamable.Media.Server
        }.getOrThrow()
    }

    private suspend fun loadBackground(mediaItem: MediaItem): Result<Streamable.Media.Background> {
        val streams = mediaItem.track.backgrounds
        val index = mediaItem.backgroundIndex
        val streamable = streams[index]
        return withClient(mediaItem) {
            loadStreamableMedia(streamable, false) as Streamable.Media.Background
        }
    }

    private suspend fun loadSubtitle(mediaItem: MediaItem): Result<Streamable.Media.Subtitle> {
        val streams = mediaItem.track.subtitles
        val index = mediaItem.subtitleIndex
        val streamable = streams[index]
        return withClient(mediaItem) {
            loadStreamableMedia(streamable, false) as Streamable.Media.Subtitle
        }
    }
}