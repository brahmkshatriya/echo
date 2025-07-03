package dev.brahmkshatriya.echo.playback.source

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.playback.source.StreamableResolver.Companion.copy

@UnstableApi
class StreamableDataSource(
    private val defaultDataSourceFactory: Lazy<DefaultDataSource.Factory>,
    private val defaultHttpDataSourceFactory: Lazy<DefaultHttpDataSource.Factory>,
    private val rawDataSourceFactory: Lazy<RawDataSource.Factory>,
) : BaseDataSource(true) {

    class Factory(
        context: Context,
    ) : DataSource.Factory {
        private val defaultDataSourceFactory = lazy {
            DefaultDataSource.Factory(context, defaultHttpDataSourceFactory.value)
        }
        private val defaultHttpDataSourceFactory = lazy {
            DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
        }
        private val rawDataSourceFactory = lazy { RawDataSource.Factory() }
        override fun createDataSource() = StreamableDataSource(
            defaultDataSourceFactory, defaultHttpDataSourceFactory, rawDataSourceFactory
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
        val result = dataSpec.customData as? Result<*>
        val (factory, spec) = when (result) {
            null -> defaultDataSourceFactory to dataSpec
            else -> when (val streamable = result.getOrThrow() as Streamable.Source) {
                is Streamable.Source.Raw -> rawDataSourceFactory to dataSpec.copy(customData = streamable)
                is Streamable.Source.Http -> {
                    val spec = streamable.request.run {
                        defaultHttpDataSourceFactory.value.setDefaultRequestProperties(headers)
                        dataSpec.copy(uri = url.toUri(), httpRequestHeaders = headers)
                    }
                    defaultDataSourceFactory to spec
                }
            }
        }
        val source = factory.value.createDataSource()
        this.source = source
        return source.open(spec)
    }
}
