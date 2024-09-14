package dev.brahmkshatriya.echo.playback

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.playback.AudioResolver.Companion.copy

@UnstableApi
class AudioDataSource(
    private val defaultDataSourceFactory: DefaultDataSource.Factory,
    private val byteStreamDataSourceFactory: ByteStreamDataSource.Factory,
    private val byteChannelDataSourceFactory: ByteChannelDataSource.Factory,
) : BaseDataSource(true) {

    class Factory(
        context: Context,
    ) : DataSource.Factory {

        private val defaultDataSourceFactory = DefaultDataSource.Factory(context)
        private val byteStreamDataSourceFactory = ByteStreamDataSource.Factory()
        private val byteChannelDataSourceFactory = ByteChannelDataSource.Factory()
        override fun createDataSource() =
            AudioDataSource(defaultDataSourceFactory, byteStreamDataSourceFactory, byteChannelDataSourceFactory)
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
        val audio = dataSpec.customData as? Streamable.Audio
        val (source, spec) = when (audio) {
            is Streamable.Audio.ByteStream -> {
                val spec = dataSpec.copy(customData = audio)
                byteStreamDataSourceFactory.createDataSource() to spec
            }

            is Streamable.Audio.Channel -> {
                val spec = dataSpec.copy(customData = audio)
                byteChannelDataSourceFactory.createDataSource() to spec
            }

            is Streamable.Audio.Http -> {
                val spec = audio.request.run {
                    dataSpec.copy(uri = url.toUri(), httpRequestHeaders = headers)
                }
                defaultDataSourceFactory.createDataSource() to spec
            }
            else -> defaultDataSourceFactory.createDataSource() to dataSpec
        }
        this.source = source
        return source.open(spec)
    }

}