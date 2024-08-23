package dev.brahmkshatriya.echo.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C.RESULT_END_OF_INPUT
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import dev.brahmkshatriya.echo.common.models.StreamableVideo
import dev.brahmkshatriya.echo.playback.TrackResolver.Companion.copy


@OptIn(UnstableApi::class)
class VideoDataSource(val factory: DefaultDataSource.Factory) : BaseDataSource(true) {

    class Factory(context: Context) : DataSource.Factory {
        private val defaultDataSourceFactory = DefaultDataSource.Factory(context)
        override fun createDataSource() = VideoDataSource(defaultDataSourceFactory)
    }

    private var source: DataSource? = null

    override fun getUri() = source?.uri ?: "".toUri()

    override fun read(buffer: ByteArray, offset: Int, length: Int) =
        source?.read(buffer, offset, length) ?: RESULT_END_OF_INPUT

    override fun close() {
        source?.close()
        source = null
    }

    override fun open(dataSpec: DataSpec): Long {
        val video = dataSpec.customData as? StreamableVideo ?: return -1
        val spec = video.request.run {
            dataSpec.copy(uri = url.toUri(), httpRequestHeaders = headers)
        }
        val source = factory.createDataSource()
        this.source = source
        return source.open(spec)
    }
}
