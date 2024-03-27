package dev.brahmkshatriya.echo.player

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.utils.tryWith
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import java.io.InputStream

@UnstableApi
class CustomDataSource(
    context: Context,
    private val extensionListFlow: ExtensionModule.ExtensionListFlow,
    private val global: Queue,
    private val throwableFlow: MutableSharedFlow<Throwable>
) : BaseDataSource(true) {

    class Factory(
        private val context: Context,
        private val extensionListFlow: ExtensionModule.ExtensionListFlow,
        private val global: Queue,
        private val throwableFlow: MutableSharedFlow<Throwable>
    ) : DataSource.Factory {
        override fun createDataSource() =
            CustomDataSource(context, extensionListFlow, global, throwableFlow)
    }

    private val defaultDataSourceFactory = DefaultDataSource.Factory(context)
    private var defaultSource: DataSource? = null
    private var inputStream: InputStream? = null
    private var trackUri: Uri? = null

    private fun getTrack(id: String) = global.getTrack(id)
        ?: throw Exception("Track not found")

    private fun getAudio(id: String): StreamableAudio {
        val track = getTrack(id)
        val client = extensionListFlow
            .getClient<TrackClient>(track.clientId)
            ?: throw Exception("Track Client ${track.clientId} not found")

        return runBlocking {
            val streamable = track.track.streamable ?: throw Exception("Streamable not found")
            tryWith(throwableFlow) {
                client.getStreamableAudio(streamable)
            } ?: throw Exception("Stream couldn't be loaded.")
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return inputStream?.read(buffer, offset, length)
            ?: defaultSource?.read(buffer, offset, length)
            ?: throw Exception("Stream not found")
    }

    override fun open(dataSpec: DataSpec): Long {
        return when (val audio = getAudio(dataSpec.uri.toString())) {
            is StreamableAudio.ByteStreamAudio -> {
                inputStream = audio.stream
                trackUri = dataSpec.uri
                C.LENGTH_UNSET.toLong()
            }

            is StreamableAudio.StreamableFile -> {
                val uri = audio.uri
                val spec = dataSpec.withUri(uri)
                val source = defaultDataSourceFactory.createDataSource()
                defaultSource = source
                source.open(spec)
            }

            is StreamableAudio.StreamableUrl -> {
                val urlHolder = audio.url
                val uri = dataSpec.withUri(urlHolder.url.toUri())
                val spec = uri.withAdditionalHeaders(urlHolder.headers)
                val source = defaultDataSourceFactory.createDataSource()
                defaultSource = source
                source.open(spec)
            }
        }
    }

    override fun getUri(): Uri? {
        return trackUri ?: defaultSource?.uri
    }

    override fun close() {
        defaultSource?.close()
        inputStream?.close()
        trackUri = null
        inputStream = null
    }
}