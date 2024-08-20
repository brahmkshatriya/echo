package dev.brahmkshatriya.echo.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C.RESULT_END_OF_INPUT
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
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

    @OptIn(UnstableApi::class)
    class DS(val factory: DefaultDataSource.Factory) : BaseDataSource(true) {

        class Factory(context: Context) : DataSource.Factory {
            private val defaultDataSourceFactory = DefaultDataSource.Factory(context)
            override fun createDataSource() = DS(defaultDataSourceFactory)
        }

        private var source: DataSource? = null

        override fun getUri() = source?.uri

        override fun read(buffer: ByteArray, offset: Int, length: Int) =
            source?.read(buffer, offset, length) ?: RESULT_END_OF_INPUT

        override fun close() {
            source?.close()
            source = null
        }

        override fun open(dataSpec: DataSpec): Long {
            val video = dataSpec.customData as? StreamableVideo ?: return 0
            val spec = video.request.run {
                dataSpec.copy(uri = url.toUri(), httpRequestHeaders = headers)
            }
            val source = factory.createDataSource()
            this.source = source
            return source.open(spec)
        }

    }

    sealed class State {
        data object Idle : State()
        data object Loading : State()
        data class Loaded(val video: StreamableVideo?) : State()
    }

    @UnstableApi
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val streamable = runBlocking {
            val state = trackResolver.video.first { it is State.Loaded }
            trackResolver.video.value = State.Idle
            state as State.Loaded
            state.video
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