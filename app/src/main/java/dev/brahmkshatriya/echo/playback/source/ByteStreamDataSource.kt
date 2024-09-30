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

    private var audio: Streamable.Audio.ByteStream? = null

    override fun read(buffer: ByteArray, offset: Int, length: Int) =
        audio!!.stream.read(buffer, offset, length)

    override fun open(dataSpec: DataSpec): Long {
        val audio = dataSpec.customData as Streamable.Audio.ByteStream
        val requestedPosition = dataSpec.position
        audio.stream.seek(requestedPosition)
        this.audio = audio
        return audio.totalBytes
    }

    override fun getUri() = audio?.hashCode().toString().toUri()
    override fun close() {
        audio?.stream?.close()
        audio = null
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
