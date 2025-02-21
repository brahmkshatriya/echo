package dev.brahmkshatriya.echo.playback.source

import android.content.SharedPreferences
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.MediaItemUtils.backgroundIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.sourcesIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.subtitleIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.exceptions.NoServersException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@UnstableApi
class StreamableLoader(
    private val settings: SharedPreferences,
    private val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
) {
    suspend fun load(mediaItem: MediaItem) = withContext(Dispatchers.IO) {
        extensionListFlow.first { it != null }
        val new = if (mediaItem.isLoaded) mediaItem
        else MediaItemUtils.buildLoaded(settings, mediaItem, loadTrack(mediaItem))

        val server = async { loadServer(new) }
        val background = async { if (new.backgroundIndex < 0) null else loadBackground(new) }
        val subtitle = async { if (new.subtitleIndex < 0) null else loadSubtitle(new) }

        MediaItemUtils.buildWithBackgroundAndSubtitle(
            new, background.await(), subtitle.await()
        ) to server.await()
    }

    private suspend fun <T> withClient(
        mediaItem: MediaItem,
        block: suspend TrackClient.() -> T
    ): T {
        val extension = extensionListFlow.getExtensionOrThrow(mediaItem.extensionId)
        return extension.get<TrackClient, T> { block() }.getOrThrow()
    }

    private suspend fun loadTrack(item: MediaItem) = withClient(item) {
        loadTrack(item.track).also {
            it.servers.ifEmpty { throw NoServersException() }
        }
    }

    private suspend fun loadServer(mediaItem: MediaItem): Streamable.Media.Server {
        val streams = mediaItem.track.servers
        val index = mediaItem.sourcesIndex
        val streamable = streams[index]
        return withClient(mediaItem) {
            loadStreamableMedia(streamable, false) as Streamable.Media.Server
        }
    }

    private suspend fun loadBackground(mediaItem: MediaItem): Streamable.Media.Background {
        val streams = mediaItem.track.backgrounds
        val index = mediaItem.backgroundIndex
        val streamable = streams[index]
        return withClient(mediaItem) {
            loadStreamableMedia(streamable, false) as Streamable.Media.Background
        }
    }

    private suspend fun loadSubtitle(mediaItem: MediaItem): Streamable.Media.Subtitle {
        val streams = mediaItem.track.subtitles
        val index = mediaItem.subtitleIndex
        val streamable = streams[index]
        return withClient(mediaItem) {
            loadStreamableMedia(streamable, false) as Streamable.Media.Subtitle
        }
    }
}