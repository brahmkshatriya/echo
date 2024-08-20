package dev.brahmkshatriya.echo.playback

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource.Resolver
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.StreamableVideo
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.audioStreamIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.videoStreamIndex
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.utils.getFromCache
import dev.brahmkshatriya.echo.utils.saveToCache
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.trackNotSupported
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

class TrackResolver(
    private val context: Context,
    private val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val settings: SharedPreferences
) : Resolver {

    lateinit var player: Player
    var video: MutableStateFlow<VideoResolver.State> = MutableStateFlow(VideoResolver.State.Idle)

    @UnstableApi
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {

        val (index, mediaItem) = runBlocking(Dispatchers.Main) {
            player.getMediaItemById(dataSpec.uri.toString())
        } ?: throw Exception(context.getString(R.string.track_not_found))

        val track = mediaItem.track
        val clientId = mediaItem.clientId

        val extension = extensionListFlow.getExtension(clientId)
        val client = extension?.client
            ?: throw Exception(context.noClient().message)

        if (client !is TrackClient)
            throw Exception(context.trackNotSupported(extension.metadata.name).message)

        val loadedTrack = if (!mediaItem.isLoaded) loadTrack(client, track) else track

        video.value = VideoResolver.State.Loading
        val streamableVideo = loadVideo(client, loadedTrack, mediaItem.videoStreamIndex).getOrElse {
            video.value = VideoResolver.State.Idle
            throw Exception(it)
        }
        video.value = VideoResolver.State.Loaded(streamableVideo)

        val newMediaItem = MediaItemUtils.build(settings, mediaItem, loadedTrack, streamableVideo)

        runBlocking(Dispatchers.Main) {
            player.replaceMediaItem(index, newMediaItem)
        }

        val streamableAudio = loadAudio(client, newMediaItem).getOrElse { throw Exception(it) }
        return dataSpec.copy(customData = streamableAudio)
    }

    private fun loadTrack(client: TrackClient, track: Track): Track {
        val id = track.id
        val loaded = getTrackFromCache(id) ?: runBlocking {
            runCatching { client.loadTrack(track) }
        }.getOrThrow()
        context.saveToCache(id, loaded)
        return loaded
    }

    private fun loadAudio(client: TrackClient, mediaItem: MediaItem): Result<StreamableAudio> {
        val streams = mediaItem.track.audioStreamables
        val index = mediaItem.audioStreamIndex
        val streamable = streams[index]
        return runBlocking {
            runCatching { client.getStreamableAudio(streamable) }
        }
    }

    private fun loadVideo(client: TrackClient, track: Track, index: Int): Result<StreamableVideo?> {
        val streams = track.videoStreamable
        val streamable =
            streams.getOrNull(index) ?: streams.firstOrNull() ?: return Result.success(null)
        return runBlocking {
            runCatching { client.getStreamableVideo(streamable) }
        }
    }

    private fun getTrackFromCache(id: String): Track? {
        val track = context.getFromCache<Track>(id) ?: return null
        return if (!track.isExpired()) track else null
    }

    private fun Track.isExpired() = System.currentTimeMillis() > expiresAt

    companion object {
        @OptIn(UnstableApi::class)
        fun DataSpec.copy(
            uri: Uri? = null,
            uriPositionOffset: Long? = null,
            httpMethod: Int? = null,
            httpBody: ByteArray? = null,
            httpRequestHeaders: Map<String, String>? = null,
            position: Long? = null,
            length: Long? = null,
            key: String? = null,
            flags: Int? = null,
            customData: Any? = null
        ): DataSpec {
            return DataSpec.Builder()
                .setUri(uri ?: this.uri)
                .setUriPositionOffset(uriPositionOffset ?: this.uriPositionOffset)
                .setHttpMethod(httpMethod ?: this.httpMethod)
                .setHttpBody(httpBody ?: this.httpBody)
                .setHttpRequestHeaders(httpRequestHeaders ?: this.httpRequestHeaders)
                .setPosition(position ?: this.position)
                .setLength(length ?: this.length)
                .setKey(key ?: this.key)
                .setFlags(flags ?: this.flags)
                .setCustomData(customData ?: this.customData)
                .build()
        }

        fun Player.getMediaItemById(id: String): Pair<Int, MediaItem>? {
            (0 until mediaItemCount).forEach { index ->
                val mediaItem = getMediaItemAt(index)
                if (mediaItem.mediaId == id) return index to mediaItem
            }
            return null
        }
    }
}