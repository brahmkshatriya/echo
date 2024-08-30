package dev.brahmkshatriya.echo.playback

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.source.CompositeMediaSource
import androidx.media3.exoplayer.source.FilteringMediaSource
import androidx.media3.exoplayer.source.MediaPeriod
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.upstream.Allocator
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.video
import dev.brahmkshatriya.echo.playback.MediaItemUtils.videoIndex
import dev.brahmkshatriya.echo.plugger.ExtensionInfo.Companion.toExtensionInfo
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.ui.exception.AppException.Companion.toAppException
import dev.brahmkshatriya.echo.utils.getFromCache
import dev.brahmkshatriya.echo.utils.saveToCache
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.trackNotSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class DelayedSource(
    private val mediaItem: MediaItem,
    private val scope: CoroutineScope,
    private val context: Context,
    private val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val settings: SharedPreferences,
    private val audioFactory: MediaFactories,
    private val videoFactory: MediaFactories,
    private val throwableFlow: MutableSharedFlow<Throwable>
) : CompositeMediaSource<Nothing>() {

    private var resolvedMediaItem: MediaItem? = null
    private lateinit var actualSource: MediaSource

    override fun prepareSourceInternal(mediaTransferListener: TransferListener?) {
        super.prepareSourceInternal(mediaTransferListener)
        scope.launch(Dispatchers.IO) {
            val new = runCatching { resolve(mediaItem) }.getOrElse {
                throwableFlow.emit(it)
                return@launch
            }
            onUrlResolved(new)
        }
    }

    private suspend fun onUrlResolved(new: MediaItem) = withContext(Dispatchers.Main) {
        resolvedMediaItem = new
        val video = new.video
        println("onUrlResolved ${new.track.title} : $video")
        actualSource = when (video) {
            is Streamable.Media.WithVideo.WithAudio -> videoFactory.create(new)
            null -> FilteringMediaSource(audioFactory.create(new), C.TRACK_TYPE_AUDIO)
            is Streamable.Media.WithVideo.Only -> if (!video.looping) MergingMediaSource(
                FilteringMediaSource(videoFactory.create(new), C.TRACK_TYPE_VIDEO),
                FilteringMediaSource(audioFactory.create(new), C.TRACK_TYPE_AUDIO)
            ) else FilteringMediaSource(audioFactory.create(new), C.TRACK_TYPE_AUDIO)
        }
        prepareChildSource(null, actualSource)
    }

    override fun getMediaItem() = resolvedMediaItem ?: mediaItem

    override fun createPeriod(
        id: MediaSource.MediaPeriodId, allocator: Allocator, startPositionUs: Long
    ) = actualSource.createPeriod(id, allocator, startPositionUs)

    override fun releasePeriod(mediaPeriod: MediaPeriod) =
        actualSource.releasePeriod(mediaPeriod)

    override fun onChildSourceInfoRefreshed(
        childSourceId: Nothing?, mediaSource: MediaSource, newTimeline: Timeline
    ) = refreshSourceInfo(newTimeline)


    private suspend fun resolve(mediaItem: MediaItem): MediaItem {
        val track = mediaItem.track
        val loadedTrack = if (!mediaItem.isLoaded) loadTrack(mediaItem) else track
        val newMediaItem = MediaItemUtils.build(settings, mediaItem, loadedTrack)
        val video = if (mediaItem.videoIndex < 0) return newMediaItem else loadVideo(mediaItem)
        return MediaItemUtils.build(newMediaItem, video)
    }

    private suspend fun loadTrack(
        item: MediaItem
    ): Track {
        val id = item.track.id
        val loaded = getTrackFromCache(id) ?: item.getTrackClient(context, extensionListFlow) {
            val track = loadTrack(item.track)
            track.audioStreamables.ifEmpty {
                track.mediaStreamables.ifEmpty {
                    throw Exception(context.getString(R.string.track_not_found))
                }
            }
            track
        }
        context.saveToCache(id, loaded)
        return loaded
    }

    private suspend fun loadVideo(mediaItem: MediaItem): Streamable.Media.WithVideo {
        val streams = mediaItem.track.videoStreamables
        val index = mediaItem.videoIndex
        val streamable = streams[index]
        return mediaItem.getTrackClient(context, extensionListFlow) {
            getStreamableMedia(streamable) as Streamable.Media.WithVideo
        }
    }

    private fun getTrackFromCache(id: String): Track? {
        val track = context.getFromCache<Track>(id) ?: return null
        return if (!track.isExpired()) track else null
    }

    private fun Track.isExpired() = System.currentTimeMillis() > expiresAt

    companion object {
        suspend fun <T> MediaItem.getTrackClient(
            context: Context,
            extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
            block: suspend TrackClient.() -> T
        ): T {
            val extension = extensionListFlow.getExtension(clientId)
            val client = extension?.client
                ?: throw Exception(context.noClient().message)

            if (client !is TrackClient)
                throw Exception(context.trackNotSupported(extension.metadata.name).message)
            val info = extension.metadata.toExtensionInfo(ExtensionType.MUSIC)
            return runCatching { block(client) }.getOrElse { throw it.toAppException(info) }
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