package dev.brahmkshatriya.echo.playback

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.playback.TrackResolver.Companion.copy

@UnstableApi
class StreamableDataSource(
    private val defaultDataSourceFactory: DefaultDataSource.Factory,
    private val byteStreamDataSourceFactory: ByteStreamDataSource.Factory,
) : BaseDataSource(true) {

    class Factory(
        context: Context,
    ) : DataSource.Factory {

        private val defaultDataSourceFactory = DefaultDataSource.Factory(context)
        private val byteStreamDataSourceFactory = ByteStreamDataSource.Factory()
        override fun createDataSource() =
            StreamableDataSource(defaultDataSourceFactory, byteStreamDataSourceFactory)
    }

    private var source: DataSource? = null

    override fun getUri() = source?.uri

    override fun read(buffer: ByteArray, offset: Int, length: Int) =
        source?.read(buffer, offset, length) ?: throw Exception("Source not opened")

    override fun close() {
        source?.close()
        source = null
    }

    override fun open(dataSpec: DataSpec): Long {
        val audio = dataSpec.customData as? StreamableAudio
            ?: throw Exception("No audio found")
        val (source, spec) = when (audio) {
            is StreamableAudio.ByteStreamAudio -> {
                val spec = dataSpec.copy(customData = audio)
                byteStreamDataSourceFactory.createDataSource() to spec
            }

            is StreamableAudio.StreamableRequest -> {
                val spec = audio.request.run {
                    dataSpec.copy(uri = url.toUri(), httpRequestHeaders = headers)
                }
                defaultDataSourceFactory.createDataSource() to spec
            }
        }
        this.source = source
        return source.open(spec)
    }

}