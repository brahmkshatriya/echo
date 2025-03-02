package dev.brahmkshatriya.echo.playback.source

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.CompositeMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaPeriod
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.extensions.Extensions
import dev.brahmkshatriya.echo.extensions.builtin.offline.OfflineExtension
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.MediaItemUtils.backgroundIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.retries
import dev.brahmkshatriya.echo.playback.MediaItemUtils.sourceIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.sourcesIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.subtitleIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.select
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.exceptions.NoSourceException
import dev.brahmkshatriya.echo.utils.CacheUtils.saveToCache
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import java.io.IOException

@UnstableApi
class StreamableMediaSource(
    private var mediaItem: MediaItem,
    private val context: Context,
    private val state: PlayerState,
    private val loader: StreamableLoader,
    private val factories: Factories,
    private val changeFlow: MutableSharedFlow<Pair<MediaItem, MediaItem>>? = null
) : CompositeMediaSource<Nothing>() {

    private var error: Throwable? = null
    override fun maybeThrowSourceInfoRefreshError() {
        error?.let { throw IOException(it) }
        super.maybeThrowSourceInfoRefreshError()
    }

    private lateinit var actualSource: MediaSource
    override fun prepareSourceInternal(mediaTransferListener: TransferListener?) {
        super.prepareSourceInternal(mediaTransferListener)
        val (new, server) = runBlocking(Dispatchers.IO) {
            runCatching { loader.load(mediaItem) }.getOrElse {
                error = it
                return@runBlocking null
            }
        } ?: return
        runBlocking { changeFlow?.emit(mediaItem to new) }
        mediaItem = new
        state.servers.apply {
            value = value.toMutableMap().apply { set(new.mediaId, server) }
        }

        if (new.extensionId != OfflineExtension.metadata.id) {
            val track = mediaItem.track
            context.saveToCache(track.id, new.extensionId to track, "track")
        }

        val sources = server.sources
        actualSource = when (sources.size) {
            0 -> run { error = NoSourceException(); return }
            1 -> factories.create(new, 0, sources.first())
            else -> {
                if (server.merged) MergingMediaSource(
                    *sources.mapIndexed { index, source ->
                        factories.create(new, index, source)
                    }.toTypedArray()
                ) else {
                    val index = mediaItem.sourceIndex
                    val source = sources.getOrNull(index) ?: sources.select(context.getSettings())
                    factories.create(new, index, source)
                }
            }
        }
        prepareChildSource(null, actualSource)

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
            if (retries != mediaItem.retries) return@run false
            if (sourcesIndex != mediaItem.sourcesIndex) return@run false
            if (sourceIndex != mediaItem.sourceIndex) return@run false
            if (backgroundIndex != mediaItem.backgroundIndex) return@run false
            if (subtitleIndex != mediaItem.subtitleIndex) return@run false
        }
        if (::actualSource.isInitialized) actualSource.canUpdateMediaItem(mediaItem)
        else false
    }

    override fun updateMediaItem(mediaItem: MediaItem) {
        this.mediaItem = mediaItem
        actualSource.updateMediaItem(mediaItem)
    }

    data class Factories(
        val dash: Lazy<MediaSource.Factory>,
        val hls: Lazy<MediaSource.Factory>,
        val default: Lazy<MediaSource.Factory>
    ) {
        fun create(mediaItem: MediaItem, index: Int, source: Streamable.Source): MediaSource {
            val type = (source as? Streamable.Source.Http)?.type
            val factory = when (type) {
                Streamable.SourceType.DASH -> dash
                Streamable.SourceType.HLS -> hls
                Streamable.SourceType.Progressive, null -> default
            }
            val new = MediaItemUtils.buildForSource(mediaItem, index, source)
            return factory.value.createMediaSource(new)
        }
    }

    class Factory(
        private val context: Context,
        private val state: PlayerState,
        extensions: Extensions,
        cache: SimpleCache,
        private val changeFlow: MutableSharedFlow<Pair<MediaItem, MediaItem>>? = null
    ) : MediaSource.Factory {

        private val loader = StreamableLoader(context.getSettings(), extensions.music)

        private val factories = Factories(
            lazily { DashMediaSource.Factory(dataSource) },
            lazily { HlsMediaSource.Factory(dataSource) },
            lazily { DefaultMediaSourceFactory(dataSource) }
        )

        private val dataSource = ResolvingDataSource.Factory(
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(StreamableDataSource.Factory(context)),
            StreamableResolver(state.servers)
        )

        private val provider = DefaultDrmSessionManagerProvider().apply {
            setDrmHttpDataSourceFactory(dataSource)
        }
        private var drmSessionManagerProvider: DrmSessionManagerProvider? = provider
        private var loadErrorHandlingPolicy: LoadErrorHandlingPolicy? = null
        private fun lazily(factory: () -> MediaSource.Factory) = lazy {
            factory().apply {
                drmSessionManagerProvider?.let { setDrmSessionManagerProvider(it) }
                loadErrorHandlingPolicy?.let { setLoadErrorHandlingPolicy(it) }
            }
        }

        override fun getSupportedTypes() = intArrayOf(
            C.CONTENT_TYPE_OTHER, C.CONTENT_TYPE_HLS, C.CONTENT_TYPE_DASH
        )

        override fun setDrmSessionManagerProvider(
            drmSessionManagerProvider: DrmSessionManagerProvider
        ): MediaSource.Factory {
            this.drmSessionManagerProvider = drmSessionManagerProvider
            return this
        }

        override fun setLoadErrorHandlingPolicy(
            loadErrorHandlingPolicy: LoadErrorHandlingPolicy
        ): MediaSource.Factory {
            this.loadErrorHandlingPolicy = loadErrorHandlingPolicy
            return this
        }

        override fun createMediaSource(mediaItem: MediaItem) =
            StreamableMediaSource(mediaItem, context, state, loader, factories, changeFlow)
    }
}