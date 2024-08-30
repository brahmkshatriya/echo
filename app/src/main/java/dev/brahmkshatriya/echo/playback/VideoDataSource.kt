package dev.brahmkshatriya.echo.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.playback.AudioResolver.Companion.copy


@OptIn(UnstableApi::class)
class VideoDataSource(val factory: DefaultDataSource.Factory) : BaseDataSource(true) {

    class Factory(context: Context) : DataSource.Factory {
        private val defaultDataSourceFactory = DefaultDataSource.Factory(context)
        override fun createDataSource() = run {
            VideoDataSource(defaultDataSourceFactory)
        }
    }

    private var source: DataSource? = null

    override fun getUri() = source?.uri

    override fun read(buffer: ByteArray, offset: Int, length: Int) =
        source?.read(buffer, offset, length)!!

    override fun close() {
        source?.close()
        source = null
    }

    override fun open(dataSpec: DataSpec): Long {
        val video = dataSpec.customData as? Streamable.Media.WithVideo
        val spec = video?.request?.run {
            dataSpec.copy(uri = url.toUri(), httpRequestHeaders = headers)
        } ?: dataSpec
        val source = factory.createDataSource()
        this.source = source
        return source.open(spec)
    }
}
