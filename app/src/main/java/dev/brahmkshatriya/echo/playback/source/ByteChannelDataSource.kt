package dev.brahmkshatriya.echo.playback.source

import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import dev.brahmkshatriya.echo.common.models.Streamable
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.discard
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.runBlocking
import java.io.IOException

@OptIn(UnstableApi::class)
class ByteChannelDataSource : BaseDataSource(true) {

    class Factory : DataSource.Factory {
        override fun createDataSource() = ByteChannelDataSource()
    }

    private var source: Streamable.Source.Channel? = null

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val channel = source!!.channel
        return runBlocking {
            val bytesRead = channel.readAvailable(buffer, offset, length)
            if (bytesRead == -1) C.RESULT_END_OF_INPUT else bytesRead
        }
    }

    override fun open(dataSpec: DataSpec): Long {
        val source = dataSpec.customData as Streamable.Source.Channel
        val requestedPosition = dataSpec.position
        runBlocking {
            source.channel.seek(requestedPosition)
        }
        this.source = source
        return source.totalBytes
    }

    override fun getUri() = source?.hashCode().toString().toUri()

    override fun close() {
        source?.channel?.cancel()
        source = null
    }

    private suspend fun ByteReadChannel.seek(requestedPosition: Long) {
        val discarded = discard(requestedPosition)
        if (discarded < requestedPosition) {
            throw IOException("Reached end of stream before desired position")
        }
    }
}