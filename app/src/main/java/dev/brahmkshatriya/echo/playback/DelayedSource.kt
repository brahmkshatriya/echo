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
import dev.brahmkshatriya.echo.playback.MediaItemUtils.audioIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.audioStreamable
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.subtitleIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.video
import dev.brahmkshatriya.echo.playback.MediaItemUtils.videoIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.videoStreamable
import dev.brahmkshatriya.echo.plugger.ExtensionInfo.Companion.toExtensionInfo
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.ui.exception.AppException.Companion.toAppException
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.trackNotSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException

@OptIn(UnstableApi::class)
class DelayedSource(
    private var mediaItem: MediaItem,
    private val scope: CoroutineScope,
    private val context: Context,
    private val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val settings: SharedPreferences,
    private val audioFactory: MediaFactories,
    private val videoFactory: MediaFactories,
    private val throwableFlow: MutableSharedFlow<Throwable>
) : CompositeMediaSource<Nothing>() {

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
        mediaItem = new
        val useVideoFactory = when (val video = new.video) {
            is Streamable.Media.WithVideo.WithAudio -> new.videoStreamable == new.audioStreamable
            is Streamable.Media.WithVideo.Only -> if (!video.looping) false else null
            null -> null
        }
        actualSource = when (useVideoFactory) {
            true -> videoFactory.create(new)
            null -> FilteringMediaSource(audioFactory.create(new), C.TRACK_TYPE_AUDIO)
            false -> MergingMediaSource(
                FilteringMediaSource(
                    videoFactory.create(new),
                    setOf(C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_TEXT)
                ),
                FilteringMediaSource(audioFactory.create(new), C.TRACK_TYPE_AUDIO)
            )
        }
        runCatching { prepareChildSource(null, actualSource) }
    }

    override fun maybeThrowSourceInfoRefreshError() {
        runCatching {
            super.maybeThrowSourceInfoRefreshError()
        }.getOrElse {
            if (it is IOException) throw it
            else runBlocking {
                if (it is NullPointerException) return@runBlocking
                throwableFlow.emit(it)
            }
        }
    }

    override fun onChildSourceInfoRefreshed(
        childSourceId: Nothing?, mediaSource: MediaSource, newTimeline: Timeline
    ) = refreshSourceInfo(newTimeline)

    override fun getMediaItem() = mediaItem

    override fun createPeriod(
        id: MediaSource.MediaPeriodId, allocator: Allocator, startPositionUs: Long
    ) = actualSource.createPeriod(id, allocator, startPositionUs)

    override fun releasePeriod(mediaPeriod: MediaPeriod) =
        actualSource.releasePeriod(mediaPeriod)

    override fun canUpdateMediaItem(mediaItem: MediaItem) = run {
        this.mediaItem.apply {
            if (audioIndex != mediaItem.audioIndex) return@run false
            if (videoIndex != mediaItem.videoIndex) return@run false
            if (subtitleIndex != mediaItem.subtitleIndex) return@run false
        }
        actualSource.canUpdateMediaItem(mediaItem)
    }

    override fun updateMediaItem(mediaItem: MediaItem) {
        this.mediaItem = mediaItem
        actualSource.updateMediaItem(mediaItem)
    }

    private suspend fun resolve(mediaItem: MediaItem): MediaItem {
        val new = if (mediaItem.isLoaded) mediaItem
        else MediaItemUtils.build(settings, mediaItem, loadTrack(mediaItem))
        val video = if (new.videoIndex < 0) null else loadVideo(new)
        val subtitle = if (new.subtitleIndex < 0) null else loadSubtitle(new)
        return MediaItemUtils.build(new, video, subtitle)
    }

    private suspend fun loadTrack(item: MediaItem) =
        item.getTrackClient(context, extensionListFlow) {
            loadTrack(item.track).also {
                it.audioStreamables.ifEmpty {
                    throw Exception(context.getString(R.string.track_not_found))
                }
            }
        }

    private suspend fun loadVideo(mediaItem: MediaItem): Streamable.Media.WithVideo {
        val streams = mediaItem.track.videoStreamables
        val index = mediaItem.videoIndex
        val streamable = streams[index]
        return mediaItem.getTrackClient(context, extensionListFlow) {
            getStreamableMedia(streamable) as Streamable.Media.WithVideo
        }
    }

    private suspend fun loadSubtitle(mediaItem: MediaItem): Streamable.Media.Subtitle {
        val streams = mediaItem.track.subtitleStreamables
        val index = mediaItem.subtitleIndex
        val streamable = streams[index]
        return mediaItem.getTrackClient(context, extensionListFlow) {
            getStreamableMedia(streamable) as Streamable.Media.Subtitle
        }
    }

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