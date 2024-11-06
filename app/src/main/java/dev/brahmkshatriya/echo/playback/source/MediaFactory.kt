package dev.brahmkshatriya.echo.playback.source

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

@UnstableApi
class MediaFactory(
    cache: SimpleCache,
    val current : MutableStateFlow<Streamable.Media.Sources?>,
    val context: Context,
    val scope: CoroutineScope,
    val extListFlow: MutableStateFlow<List<MusicExtension>?>,
    val settings: SharedPreferences,
    val throwableFlow: MutableSharedFlow<Throwable>
) : MediaSource.Factory {

    private val mediaResolver = MediaResolver(current)
    private val dataSource = ResolvingDataSource.Factory(
        CustomCacheDataSource.Factory(cache, MediaDataSource.Factory(context)),
        mediaResolver
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

    override fun createMediaSource(mediaItem: MediaItem) = DelayedSource(mediaItem, this)

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