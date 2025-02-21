package dev.brahmkshatriya.echo.playback.source

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
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
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.extensions.builtin.offline.OfflineExtension
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.MediaItemUtils.backgroundIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.sourceIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.sourcesIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.subtitleIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.select
import dev.brahmkshatriya.echo.playback.exceptions.NoSourceException
import dev.brahmkshatriya.echo.utils.CacheUtils.saveToCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

@UnstableApi
class StreamableMediaSource(
    private val context: Context,
    private val scope: CoroutineScope,
    private val settings: SharedPreferences,
    private val current: MutableStateFlow<Map<String, Streamable.Media.Server>>,
    private val loader: StreamableLoader,
    private val dash: Lazy<MediaSource.Factory>,
    private val hls: Lazy<MediaSource.Factory>,
    private val default: Lazy<MediaSource.Factory>,
    private var mediaItem: MediaItem
) : CompositeMediaSource<Nothing>() {

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

    private var error: Throwable? = null
    override fun maybeThrowSourceInfoRefreshError() {
        error?.let { throw IOException(it) }
        super.maybeThrowSourceInfoRefreshError()
    }

    private lateinit var actualSource: MediaSource
    override fun prepareSourceInternal(mediaTransferListener: TransferListener?) {
        super.prepareSourceInternal(mediaTransferListener)
        val handler = Util.createHandlerForCurrentLooper()
        scope.launch {
            val (new, server) = runCatching { loader.load(mediaItem) }.getOrElse {
                error = it
                return@launch
            }
            mediaItem = new
            current.apply {
                value = value.toMutableMap().apply { set(new.mediaId, server) }
            }

            if (new.extensionId != OfflineExtension.metadata.id) {
                val track = mediaItem.track
                context.saveToCache(track.id, new.extensionId to track, "track")
            }

            val sources = server.sources
            actualSource = when (sources.size) {
                0 -> {
                    error = NoSourceException()
                    return@launch
                }

                1 -> create(new, 0, sources.first())
                else -> {
                    if (server.merged) MergingMediaSource(
                        *sources.mapIndexed { index, source ->
                            create(new, index, source)
                        }.toTypedArray()
                    ) else {
                        val index = mediaItem.sourceIndex
                        val source = sources.getOrNull(index) ?: sources.select(settings)
                        create(new, index, source)
                    }
                }
            }
            handler.post {
                runCatching { prepareChildSource(null, actualSource) }
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
        if (::actualSource.isInitialized) actualSource.canUpdateMediaItem(mediaItem)
        else false
    }

    override fun updateMediaItem(mediaItem: MediaItem) {
        this.mediaItem = mediaItem
        actualSource.updateMediaItem(mediaItem)
    }

    @UnstableApi
    class Factory(
        private val context: Context,
        private val scope: CoroutineScope,
        private val settings: SharedPreferences,
        private val current: MutableStateFlow<Map<String, Streamable.Media.Server>>,
        extListFlow: MutableStateFlow<List<MusicExtension>?>,
        cache: SimpleCache,
    ) : MediaSource.Factory {

        private val dataSource = ResolvingDataSource.Factory(
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(StreamableDataSource.Factory(context)),
            StreamableResolver(current)
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

        private val default = lazily { DefaultMediaSourceFactory(dataSource) }
        private val hls = lazily { HlsMediaSource.Factory(dataSource) }
        private val dash = lazily { DashMediaSource.Factory(dataSource) }
        private val loader = StreamableLoader(settings, extListFlow)

        override fun createMediaSource(mediaItem: MediaItem) = StreamableMediaSource(
            context, scope, settings, current, loader, dash, hls, default, mediaItem
        )
    }
}