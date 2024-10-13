package dev.brahmkshatriya.echo.playback.source

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
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.offline.OfflineExtension
import dev.brahmkshatriya.echo.playback.MediaItemUtils.audioIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.toIdAndIsVideo
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.video
import dev.brahmkshatriya.echo.playback.source.DelayedSource.Companion.getMediaItemById
import dev.brahmkshatriya.echo.playback.source.DelayedSource.Companion.getTrackClient
import dev.brahmkshatriya.echo.playback.source.MediaDataSource.Companion.copy
import dev.brahmkshatriya.echo.utils.saveToCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

class MediaResolver(
    private val context: Context,
    private val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
) : Resolver {

    lateinit var player: Player

    @UnstableApi
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val (id, isVideo) = dataSpec.uri.toString().toIdAndIsVideo() ?: return dataSpec

        val (_, mediaItem) = runBlocking(Dispatchers.Main) {
            player.getMediaItemById(id)
        } ?: throw Exception(context.getString(R.string.track_not_found))

        val streamable = if (isVideo) mediaItem.video!! else
            runBlocking(Dispatchers.IO) { runCatching { loadAudio(mediaItem) } }.getOrThrow()

        val uri = if (mediaItem.clientId == OfflineExtension.metadata.id) LOCAL
        else {
            if(!isVideo) {
                val track = mediaItem.track
                context.saveToCache(track.id, mediaItem.clientId to track, "track")
            }
            dataSpec.uri
        }
        return dataSpec.copy(
            uri = uri,
            customData = streamable
        )
    }

    private suspend fun loadAudio(mediaItem: MediaItem): Streamable.Media {
        val streams = mediaItem.track.audioStreamables
        val index = mediaItem.audioIndex
        val streamable = streams[index]
        return mediaItem.getTrackClient(context, extensionListFlow) {
            when (val media = getStreamableMedia(streamable)) {
                is Streamable.Media.AudioOnly -> media
                is Streamable.Media.WithVideo.WithAudio -> media
                else -> throw IllegalStateException("Invalid streamable type : ${media::class}")
            }
        }
    }

    companion object {

        val LOCAL = "local".toUri()

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