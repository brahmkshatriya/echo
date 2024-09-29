package dev.brahmkshatriya.echo.playback.source

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.MediaItemUtils.getStreamable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

@UnstableApi
class MediaFactory(
    cache: SimpleCache,
    private val context: Context,
    private val scope: CoroutineScope,
    private val extListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val settings: SharedPreferences,
    private val throwableFlow: MutableSharedFlow<Throwable>
) : MediaSource.Factory {

    private var drmSessionManagerProvider: DrmSessionManagerProvider? = null
    private var loadErrorHandlingPolicy: LoadErrorHandlingPolicy? = null
    private fun lazily(factory: () -> MediaSource.Factory) = lazy {
        factory().apply {
            drmSessionManagerProvider?.let { setDrmSessionManagerProvider(it) }
            loadErrorHandlingPolicy?.let { setLoadErrorHandlingPolicy(it) }
        }
    }

    private lateinit var player: Player
    fun setPlayer(player: Player) {
        this.player = player
        mediaResolver.player = player
    }

    private val mediaResolver = MediaResolver(context, extListFlow)
    private val dataSource = ResolvingDataSource.Factory(
        CacheDataSource
            .Factory().setCache(cache)
            .setUpstreamDataSourceFactory(MediaDataSource.Factory(context)),
        mediaResolver
    )
    private val default = lazily { DefaultMediaSourceFactory(dataSource) }
    private val hls = lazily { HlsMediaSource.Factory(dataSource) }
    private val dash = lazily { DashMediaSource.Factory(dataSource) }

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

    override fun createMediaSource(mediaItem: MediaItem) = DelayedSource(
        mediaItem, scope, context, extListFlow, settings, this, throwableFlow
    )

    fun create(mediaItem: MediaItem, isVideo: Boolean): MediaSource {
        val new = MediaItemUtils.build(mediaItem, isVideo)
        val factory = when (new.getStreamable(isVideo).mimeType) {
            Streamable.MimeType.Progressive -> default
            Streamable.MimeType.HLS -> hls
            Streamable.MimeType.DASH -> dash
        }
        return factory.value.createMediaSource(new)
    }
}