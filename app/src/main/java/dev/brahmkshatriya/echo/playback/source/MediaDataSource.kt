package dev.brahmkshatriya.echo.playback.source

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import dev.brahmkshatriya.echo.common.models.Streamable

@UnstableApi
class MediaDataSource(
    private val defaultDataSourceFactory: Lazy<DefaultDataSource.Factory>,
    private val byteStreamDataSourceFactory: Lazy<ByteStreamDataSource.Factory>,
    private val byteChannelDataSourceFactory: Lazy<ByteChannelDataSource.Factory>,
) : BaseDataSource(true) {

    class Factory(
        context: Context,
    ) : DataSource.Factory {

        private val defaultDataSourceFactory = lazy { DefaultDataSource.Factory(context) }
        private val byteStreamDataSourceFactory = lazy { ByteStreamDataSource.Factory() }
        private val byteChannelDataSourceFactory = lazy { ByteChannelDataSource.Factory() }
        override fun createDataSource() = MediaDataSource(
            defaultDataSourceFactory, byteStreamDataSourceFactory, byteChannelDataSourceFactory
        )
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
        val streamable = dataSpec.customData as? Streamable.Media
        val (factory, spec) = when (streamable) {
            is Streamable.Media.AudioOnly -> when(val audio = streamable.audio){
                is Streamable.Audio.ByteStream -> {
                    val spec = dataSpec.copy(customData = audio)
                    byteStreamDataSourceFactory to spec
                }

                is Streamable.Audio.Channel -> {
                    val spec = dataSpec.copy(customData = audio)
                    byteChannelDataSourceFactory to spec
                }

                is Streamable.Audio.Http -> {
                    val spec = audio.request.run {
                        dataSpec.copy(uri = Uri.encode(url).toUri(), httpRequestHeaders = headers)
                    }
                    defaultDataSourceFactory to spec
                }
            }
            is Streamable.Media.WithVideo -> {
                val spec = streamable.request.run {
                    dataSpec.copy(uri = Uri.encode(url).toUri(), httpRequestHeaders = headers)
                }
                defaultDataSourceFactory to spec
            }

            is Streamable.Media.Subtitle -> throw IllegalStateException()
            null -> defaultDataSourceFactory to dataSpec
        }
        val source = factory.value.createDataSource()
        this.source = source
        return source.open(spec)
    }

    companion object {
        @OptIn(UnstableApi::class)
        fun DataSpec.copy(
            uri: Uri? = null,
            uriPositionOffset: Long? = null,
            httpMethod: Int? = null,
            httpBody: ByteArray? = null,
            httpRequestHeaders: Map<String, String>? = null,
            position: Long? = null,
            length: Long? = null,
            key: String? = null,
            flags: Int? = null,
            customData: Any? = null
        ): DataSpec {
            return DataSpec.Builder()
                .setUri(uri ?: this.uri)
                .setUriPositionOffset(uriPositionOffset ?: this.uriPositionOffset)
                .setHttpMethod(httpMethod ?: this.httpMethod)
                .setHttpBody(httpBody ?: this.httpBody)
                .setHttpRequestHeaders(httpRequestHeaders ?: this.httpRequestHeaders)
                .setPosition(position ?: this.position)
                .setLength(length ?: this.length)
                .setKey(key ?: this.key)
                .setFlags(flags ?: this.flags)
                .setCustomData(customData ?: this.customData)
                .build()
        }
    }
}