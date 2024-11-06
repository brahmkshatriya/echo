package dev.brahmkshatriya.echo.playback.source

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
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
import dev.brahmkshatriya.echo.offline.OfflineExtension
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.MediaItemUtils.backgroundIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.sourceIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.sourcesIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.subtitleIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.ui.exception.AppException.Companion.toAppException
import dev.brahmkshatriya.echo.utils.saveToCache
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.trackNotSupported
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException

@OptIn(UnstableApi::class)
class DelayedSource(
    private var mediaItem: MediaItem,
    private val mediaFactory: MediaFactory,
) : CompositeMediaSource<Nothing>() {

    private val scope = mediaFactory.scope
    private val context = mediaFactory.context
    private val currentSources = mediaFactory.current
    private val extensionListFlow = mediaFactory.extListFlow
    private val settings = mediaFactory.settings
    private val throwableFlow = mediaFactory.throwableFlow

    private lateinit var actualSource: MediaSource
    override fun prepareSourceInternal(mediaTransferListener: TransferListener?) {
        super.prepareSourceInternal(mediaTransferListener)
        scope.launch(Dispatchers.IO) {
            val (new, streamable) = runCatching { resolve(mediaItem) }.getOrElse {
                throwableFlow.emit(it)
                return@launch
            }
            currentSources.value = streamable
            onUrlResolved(new, streamable)
        }
    }

    private suspend fun onUrlResolved(
        new: MediaItem, streamable: Streamable.Media.Sources,
    ) = withContext(Dispatchers.Main) {
        mediaItem = new

        if(new.clientId != OfflineExtension.metadata.id) {
            val track = mediaItem.track
            context.saveToCache(track.id, new.clientId to track, "track")
        }

        val sources = streamable.sources
        actualSource = when (sources.size) {
            0 -> throw Exception(context.getString(R.string.streamable_not_found))
            1 -> mediaFactory.create(new, 0, sources.first())
            else -> {
                if(streamable.merged) MergingMediaSource(
                    *sources.mapIndexed { index, source ->
                        mediaFactory.create(new, index, source)
                    }.toTypedArray()
                ) else {
                    val index = mediaItem.sourceIndex
                    val source = sources[index]
                    mediaFactory.create(new, index, source)
                }
            }
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
            if (sourcesIndex != mediaItem.sourcesIndex) return@run false
            if (sourceIndex != mediaItem.sourceIndex) return@run false
            if (backgroundIndex != mediaItem.backgroundIndex) return@run false
            if (subtitleIndex != mediaItem.subtitleIndex) return@run false
        }
        actualSource.canUpdateMediaItem(mediaItem)
    }

    override fun updateMediaItem(mediaItem: MediaItem) {
        this.mediaItem = mediaItem
        actualSource.updateMediaItem(mediaItem)
    }

    private suspend fun resolve(mediaItem: MediaItem) = coroutineScope {
        extensionListFlow.first { it != null }
        val new = if (mediaItem.isLoaded) mediaItem
        else MediaItemUtils.buildLoaded(settings, mediaItem, loadTrack(mediaItem))
        val sources = async { loadSources(new) }
        val background = async { if (new.backgroundIndex < 0) null else loadVideo(new) }
        val subtitle = async { if (new.subtitleIndex < 0) null else loadSubtitle(new) }
        MediaItemUtils.buildExternal(new, background.await(), subtitle.await()) to sources.await()
    }

    private suspend fun loadTrack(item: MediaItem) =
        item.getTrackClient(context, extensionListFlow) {
            loadTrack(item.track).also {
                it.sources.ifEmpty {
                    throw Exception(context.getString(R.string.no_streams_found))
                }
            }
        }

    private suspend fun loadSources(mediaItem: MediaItem): Streamable.Media.Sources {
        val streams = mediaItem.track.sources
        val index = mediaItem.sourcesIndex
        val streamable = streams[index]
        return mediaItem.getTrackClient(context, extensionListFlow) {
            getStreamableMedia(streamable) as Streamable.Media.Sources
        }
    }

    private suspend fun loadVideo(mediaItem: MediaItem): Streamable.Media.Background {
        val streams = mediaItem.track.backgrounds
        val index = mediaItem.backgroundIndex
        val streamable = streams[index]
        return mediaItem.getTrackClient(context, extensionListFlow) {
            getStreamableMedia(streamable) as Streamable.Media.Background
        }
    }

    private suspend fun loadSubtitle(mediaItem: MediaItem): Streamable.Media.Subtitle {
        val streams = mediaItem.track.subtitles
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
    }
}