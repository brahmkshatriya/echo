package dev.brahmkshatriya.echo.player

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.utils.tryWith
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import java.io.InputStream

@UnstableApi
class DataSourceFactory(
    context: Context,
    private val global: Queue,
    private val throwableFlow: MutableSharedFlow<Throwable>
) : MediaSource.Factory {

    private fun getTrack(id: String) = global.getTrack(id)
        ?: throw Exception("Track not found")

    private fun getAudio(id: String): StreamableAudio {
        val track = getTrack(id)
        val client = track.client
        return runBlocking {
            val streamable = track.track.streamable ?: throw Exception("Streamable not found")
            tryWith(throwableFlow) {
                client.getStreamableAudio(streamable)
            }!!
        }
    }

    private val httpDataSourceFactory = DefaultDataSource.Factory(context)
    private val dataSourceFactory =
        ResolvingDataSource.Factory(httpDataSourceFactory) { dataSpec: DataSpec ->
            when (val audio = getAudio(dataSpec.uri.toString())) {
                is StreamableAudio.StreamableFile -> dataSpec.withUri(audio.uri)
                is StreamableAudio.StreamableUrl -> {
                    val url = audio.url
                    val spec = dataSpec.withUri(url.url.toUri())
                    spec.withAdditionalHeaders(url.headers)
                }

                else -> dataSpec
            }
        }

    val factory = DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory)

    override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider) =
        factory.setDrmSessionManagerProvider(drmSessionManagerProvider)


    override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy) =
        factory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)

    override fun getSupportedTypes() = factory.supportedTypes

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val track = getTrack(mediaItem.mediaId)
        return when (track.track.streamable?.type) {
            Streamable.Type.URL -> factory.createMediaSource(mediaItem)
            Streamable.Type.FILE -> factory.createMediaSource(mediaItem)
            Streamable.Type.STREAM -> customFactory.createMediaSource(mediaItem)
            null -> throw Exception("Streamable not found")
        }
    }

    private val customDataSource = object : BaseDataSource(true) {
        private var inputStream: InputStream? = null
        private var trackUri: Uri? = null

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            return inputStream?.read(buffer, offset, length)
                ?: throw Exception("Stream not found")
        }

        override fun open(dataSpec: DataSpec): Long {
            val streamableAudio = getAudio(dataSpec.uri.toString())
            val audio = streamableAudio as? StreamableAudio.ByteStreamAudio
                ?: throw Exception("Streamable not found")
            inputStream = audio.stream
            trackUri = dataSpec.uri
            return C.LENGTH_UNSET.toLong()
        }

        override fun getUri(): Uri? {
            return trackUri
        }

        override fun close() {
            inputStream?.close()
            inputStream = null
        }

    }
    private val customFactory = ProgressiveMediaSource.Factory { customDataSource }

}