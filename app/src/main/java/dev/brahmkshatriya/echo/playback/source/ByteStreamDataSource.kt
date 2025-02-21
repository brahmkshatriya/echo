package dev.brahmkshatriya.echo.playback.source

import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import dev.brahmkshatriya.echo.common.models.Streamable
import java.io.InputStream

@OptIn(UnstableApi::class)
class ByteStreamDataSource : BaseDataSource(true) {

    class Factory : DataSource.Factory {
        override fun createDataSource() = ByteStreamDataSource()
    }

    private var source: Streamable.Source.ByteStream? = null

    override fun read(buffer: ByteArray, offset: Int, length: Int) =
        source!!.stream.read(buffer, offset, length)

    override fun open(dataSpec: DataSpec): Long {
        val source = dataSpec.customData as Streamable.Source.ByteStream
        val requestedPosition = dataSpec.position
        source.stream.seek(requestedPosition)
        this.source = source
        return source.totalBytes
    }

    override fun getUri() = source?.hashCode().toString().toUri()
    override fun close() {
        source?.stream?.close()
        source = null
    }

    private fun InputStream.seek(requestedPosition: Long) {
        var position = 0L
        while (position < requestedPosition) {
            val skipped = skip(requestedPosition - position)
            if (skipped == 0L) break
            position += skipped
        }
    }
}
