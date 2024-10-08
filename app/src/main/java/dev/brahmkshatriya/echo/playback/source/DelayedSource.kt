package dev.brahmkshatriya.echo.playback.source

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.source.CompositeMediaSource
import androidx.media3.exoplayer.source.MediaPeriod
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.upstream.Allocator
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.MediaItemUtils.audioIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isAudioAndVideoMerged
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.subtitleIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.videoIndex
import dev.brahmkshatriya.echo.ui.exception.AppException.Companion.toAppException
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.trackNotSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
    private val mediaFactory: MediaFactory,
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
        actualSource = when (new.isAudioAndVideoMerged()) {
            true -> mediaFactory.create(new, true)
            null -> mediaFactory.create(new, false)
            false -> MergingMediaSource(
                mediaFactory.create(new, true),
                mediaFactory.create(new, false)
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
        extensionListFlow.first { it != null }
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
                ?: throw Exception(context.noClient().message)
            val client = extension.instance.value.getOrNull()
            if (client !is TrackClient)
                throw Exception(context.trackNotSupported(extension.metadata.name).message)
            return runCatching { block(client) }.getOrElse { throw it.toAppException(extension) }
        }

        fun Player.getMediaItemById(id: String): Pair<Int, MediaItem>? {
            for (i in 0 until mediaItemCount) {
                val mediaItem = getMediaItemAt(i)
                if (mediaItem.mediaId == id) return i to mediaItem
            }
            return null
        }

    }
}