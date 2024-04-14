package dev.brahmkshatriya.echo.player

import android.content.Context
import android.content.SharedPreferences
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
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.ui.settings.AudioFragment
import kotlinx.coroutines.runBlocking
import java.io.InputStream

@UnstableApi
class StreamableDataSource(
    private val context: Context,
    private val defaultDataSourceFactory: DefaultDataSource.Factory,
    private val extensionListFlow: ExtensionModule.ExtensionListFlow,
    private val settings: SharedPreferences
) : BaseDataSource(true) {

    class Factory(
        private val context: Context,
        private val extensionListFlow: ExtensionModule.ExtensionListFlow,
        private val settings: SharedPreferences
    ) : DataSource.Factory {

        private val defaultDataSourceFactory = DefaultDataSource.Factory(context)
        override fun createDataSource() =
            StreamableDataSource(context, defaultDataSourceFactory, extensionListFlow, settings)
    }

    private var defaultSource: DataSource? = null
    private var inputStream: InputStream? = null
    private var trackUri: Uri? = null

    private fun getAudio(dataSpec: DataSpec): StreamableAudio {
        val streamableTrack = dataSpec.customData as? Queue.StreamableTrack
            ?: throw Exception("Track audio not found")
        val client = extensionListFlow.getClient(streamableTrack.clientId)
            ?: throw Exception("Client not found")
        if (client !is TrackClient)
            throw Exception("Client not supported")
        val track = streamableTrack.loaded
            ?: throw Exception("Track not loaded")

        val streamable = selectStream(track.streamables)
            ?: throw Exception(context.getString(R.string.no_streams_found))

        return runBlocking {
            runCatching { client.getStreamableAudio(streamable) }
        }.getOrThrow()
    }

    private fun selectStream(streamables: List<Streamable>) =
        when (settings.getString(AudioFragment.AudioPreference.STREAM_QUALITY, "lowest")) {
            "highest" -> streamables.maxByOrNull { it.quality }
            "medium" -> streamables.sortedBy { it.quality }.getOrNull(streamables.size / 2)
            "lowest" -> streamables.minByOrNull { it.quality }
            else -> streamables.firstOrNull()
        }


    override fun open(dataSpec: DataSpec): Long {
        trackUri = dataSpec.uri
        return when (val audio = getAudio(dataSpec)) {
            is StreamableAudio.ByteStreamAudio -> {
                inputStream = audio.stream
                C.LENGTH_UNSET.toLong()
            }

            is StreamableAudio.StreamableFile -> {
                val uri = audio.uri
                val spec = dataSpec.withUri(uri)
                val source = defaultDataSourceFactory.createDataSource()
                defaultSource = source
                source.open(spec)
            }

            is StreamableAudio.StreamableRequest -> {
                val request = audio.request
                val spec = dataSpec
                    .withUri(request.url.toUri())
                    .withRequestHeaders(request.headers)
                val source = defaultDataSourceFactory.createDataSource()
                defaultSource = source
                source.open(spec)
            }
        }
    }

    override fun getUri() = trackUri
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return inputStream?.read(buffer, offset, length)
            ?: defaultSource?.read(buffer, offset, length)
            ?: throw Exception("Stream not found")
    }

    override fun close() {
        defaultSource?.close()
        inputStream?.close()
        defaultSource = null
        inputStream = null
        trackUri = null
    }
}