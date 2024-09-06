package dev.brahmkshatriya.echo.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource.Resolver
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.playback.AudioResolver.Companion.copy
import dev.brahmkshatriya.echo.playback.DelayedSource.Companion.getMediaItemById
import dev.brahmkshatriya.echo.playback.MediaItemUtils.video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class VideoResolver(
    private val context: Context
) : Resolver {

    lateinit var player: Player

    @UnstableApi
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val id = dataSpec.uri.toString().takeIf { it.startsWith("id://") }
            ?.substringAfter("id://") ?: return dataSpec

        val (_, mediaItem) = runBlocking(Dispatchers.Main) {
            player.getMediaItemById(id)
        } ?: throw Exception(context.getString(R.string.track_not_found))

        val streamableVideo = mediaItem.video!!
        return dataSpec.copy(
            uri = streamableVideo.hashCode().toString().toUri(),
            customData = streamableVideo
        )
    }

    companion object {
        @OptIn(UnstableApi::class)
        fun getPlayer(
            context: Context, cache: SimpleCache, video: Streamable.Media.WithVideo.Only
        ): ExoPlayer {
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