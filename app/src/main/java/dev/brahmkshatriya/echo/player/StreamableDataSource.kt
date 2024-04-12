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
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.trackNotSupported
import kotlinx.coroutines.runBlocking
import java.io.InputStream

@UnstableApi
class StreamableDataSource(
    private val context: Context,
    private val extensionListFlow: ExtensionModule.ExtensionListFlow,
    private val global: Queue
) : BaseDataSource(true) {

    class Factory(
        private val context: Context,
        private val extensionListFlow: ExtensionModule.ExtensionListFlow,
        private val global: Queue
    ) : DataSource.Factory {
        override fun createDataSource() =
            StreamableDataSource(context, extensionListFlow, global)
    }

    private val defaultDataSourceFactory = DefaultDataSource.Factory(context)
    private var defaultSource: DataSource? = null
    private var inputStream: InputStream? = null
    private var trackUri: Uri? = null

    private fun getTrack(id: String) = global.getTrack(id)
        ?: throw Exception("Track not found")

    private suspend fun getAudio(id: String): StreamableAudio {
        val streamableTrack = getTrack(id)
        val client = extensionListFlow.getClient(streamableTrack.clientId)
            ?: throw Exception(context.noClient().message)
        if (client !is TrackClient)
            throw Exception(context.trackNotSupported(client.metadata.name).message)
        val streamable = streamableTrack.streamable
            ?: throw Exception(context.getString(R.string.streamable_not_found))
        return client.getStreamableAudio(streamable)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return inputStream?.read(buffer, offset, length)
            ?: defaultSource?.read(buffer, offset, length)
            ?: throw Exception("Stream not found")
    }

    override fun open(dataSpec: DataSpec): Long {
        val audio = runBlocking {
            runCatching { getAudio(dataSpec.uri.toString()) }
        }.getOrThrow()
        return when (audio) {
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
                val urlHolder = audio.urlHolder
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