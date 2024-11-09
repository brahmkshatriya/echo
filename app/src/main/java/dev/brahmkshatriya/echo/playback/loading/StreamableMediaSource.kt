package dev.brahmkshatriya.echo.playback.loading

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.TransferListener
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
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.offline.OfflineExtension
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.MediaItemUtils.backgroundIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.sourceIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.sourcesIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.subtitleIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.utils.saveToCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@UnstableApi
class StreamableMediaSource(
    private var mediaItem: MediaItem,
    private val factory: Factory,
) : CompositeMediaSource<Nothing>() {

    private var error: Throwable? = null
    override fun maybeThrowSourceInfoRefreshError() {
        error?.let { throw it }
        super.maybeThrowSourceInfoRefreshError()
    }

    private val context = factory.context
    private val scope = factory.scope
    private val current = factory.current
    private val loader = factory.loader

    override fun prepareSourceInternal(mediaTransferListener: TransferListener?) {
        super.prepareSourceInternal(mediaTransferListener)
        val handler = Util.createHandlerForCurrentLooper()
        scope.launch {
            val (item, sources) = runCatching { loader.load(mediaItem) }.getOrElse {
                error = it
                return@launch
            }
            onResolved(item, sources)
            handler.post { prepareChildSource(null, actualSource) }
        }
    }

    private lateinit var actualSource: MediaSource
    private fun onResolved(
        new: MediaItem, streamable: Streamable.Media.Sources,
    ) {
        mediaItem = new
        current.value = streamable
        if (new.clientId != OfflineExtension.metadata.id) {
            val track = mediaItem.track
            context.saveToCache(track.id, new.clientId to track, "track")
        }
        val sources = streamable.sources
        actualSource = when (sources.size) {
            0 -> throw Exception(context.getString(R.string.streamable_not_found))
            1 -> factory.create(new, 0, sources.first())
            else -> {
                if (streamable.merged) MergingMediaSource(
                    *sources.mapIndexed { index, source ->
                        factory.create(new, index, source)
                    }.toTypedArray()
                ) else {
                    val index = mediaItem.sourceIndex
                    val source = sources[index]
                    factory.create(new, index, source)
                }
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


    @UnstableApi
    class Factory(
        val context: Context,
        val scope: CoroutineScope,
        val current: MutableStateFlow<Streamable.Media.Sources?>,
        extListFlow: MutableStateFlow<List<MusicExtension>?>,
        cache: SimpleCache,
        settings: SharedPreferences,
    ) : MediaSource.Factory {

        private val dataSource = ResolvingDataSource.Factory(
            CustomCacheDataSource.Factory(cache, StreamableDataSource.Factory(context)),
            StreamableResolver(current)
        )

        private val default = lazily { DefaultMediaSourceFactory(dataSource) }
        private val hls = lazily { HlsMediaSource.Factory(dataSource) }
        private val dash = lazily { DashMediaSource.Factory(dataSource) }

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

        val loader = StreamableLoader(context, settings, extListFlow)
        override fun createMediaSource(mediaItem: MediaItem) =
            StreamableMediaSource(mediaItem, this)
    }
}