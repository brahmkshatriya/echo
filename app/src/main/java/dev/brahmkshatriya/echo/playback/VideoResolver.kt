package dev.brahmkshatriya.echo.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource.Resolver
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dev.brahmkshatriya.echo.common.models.StreamableVideo
import dev.brahmkshatriya.echo.playback.TrackResolver.Companion.copy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class VideoResolver(
    private val trackResolver: TrackResolver,
) : Resolver {

    @UnstableApi
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val streamable = runBlocking {
            trackResolver.video.first()
        }?.takeIf { !it.looping }
        return dataSpec.copy(
            customData = streamable,
        )
    }

    companion object {
        @OptIn(UnstableApi::class)
        fun getPlayer(context: Context, cache: SimpleCache, video: StreamableVideo): ExoPlayer {
            val cacheFactory = CacheDataSource
                .Factory().setCache(cache)
                .setUpstreamDataSourceFactory(
                    DefaultHttpDataSource.Factory()
                        .setDefaultRequestProperties(video.request.headers)
                )
            val factory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(cacheFactory)
            val player = ExoPlayer.Builder(context).setMediaSourceFactory(factory).build()
            player.setMediaItem(MediaItem.fromUri(video.request.url.toUri()))
            player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
            player.volume = 0f
            player.prepare()
            player.play()
            return player
        }
    }
}