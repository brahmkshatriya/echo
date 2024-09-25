package dev.brahmkshatriya.echo.playback

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import dev.brahmkshatriya.echo.playback.MediaItemUtils.audioIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.videoIndex
import dev.brahmkshatriya.echo.common.MusicExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(UnstableApi::class)
class EchoMediaSourceFactory(
    cache: SimpleCache,
    private val context: Context,
    private val scope: CoroutineScope,
    private val extListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val settings: SharedPreferences,
    private val throwableFlow: MutableSharedFlow<Throwable>
) : MediaSource.Factory {

    private lateinit var player: Player
    fun setPlayer(player: Player) {
        this.player = player
        audioResolver.player = player
        videoResolver.player = player
    }

    private val audioResolver = AudioResolver(context, extListFlow)
    private val videoResolver = VideoResolver(context)
    private val audioSource = ResolvingDataSource.Factory(
        CacheDataSource
            .Factory().setCache(cache)
            .setUpstreamDataSourceFactory(AudioDataSource.Factory(context)),
        audioResolver
    )
    private val videoSource = ResolvingDataSource.Factory(
        CacheDataSource
            .Factory().setCache(cache)
            .setUpstreamDataSourceFactory(VideoDataSource.Factory(context)),
        videoResolver
    )

    private val audioFactory = MediaFactories(audioSource) {
        it.track.audioStreamables[it.audioIndex]
    }
    private val videoFactory = MediaFactories(videoSource) {
        it.track.videoStreamables[it.videoIndex]
    }

    override fun setDrmSessionManagerProvider(
        drmSessionManagerProvider: DrmSessionManagerProvider
    ): MediaSource.Factory {
        audioFactory.setDrm(drmSessionManagerProvider)
        videoFactory.setDrm(drmSessionManagerProvider)
        return this
    }

    override fun setLoadErrorHandlingPolicy(
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy
    ): MediaSource.Factory {
        audioFactory.setPolicy(loadErrorHandlingPolicy)
        videoFactory.setPolicy(loadErrorHandlingPolicy)
        return this
    }

    override fun getSupportedTypes() = videoFactory.supportedTypes

    override fun createMediaSource(mediaItem: MediaItem) = DelayedSource(
        mediaItem, scope, context, extListFlow, settings, audioFactory, videoFactory, throwableFlow
    )
}