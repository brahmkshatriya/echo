package dev.brahmkshatriya.echo.playback

import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import dev.brahmkshatriya.echo.common.models.Streamable
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.math.min

@UnstableApi
class ByteChannelDataSource : BaseDataSource(true) {

    class Factory : DataSource.Factory {
        override fun createDataSource() = ByteChannelDataSource()
    }

    private var audio: Streamable.Audio.Channel? = null
    private var channel: ByteReadChannel? = null

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val channel = channel ?: throw IOException("Channel is not open")
        return runBlocking {
            val bytesRead = channel.readAvailable(buffer, offset, length)
            if (bytesRead == -1) C.RESULT_END_OF_INPUT else bytesRead
        }
    }

    override fun open(dataSpec: DataSpec): Long {
        val audio = dataSpec.customData as Streamable.Audio.Channel
        val requestedPosition = dataSpec.position

        channel = audio.channel
        this.audio = audio

        runBlocking {
            seekChannelToPosition(channel!!, requestedPosition)
        }

        return audio.totalBytes - requestedPosition
    }

    override fun getUri() = audio?.hashCode().toString().toUri()

    override fun close() {
        runBlocking {
            channel?.cancel()
        }
        channel = null
        audio = null
    }

    private suspend fun seekChannelToPosition(channel: ByteReadChannel, requestedPosition: Long) {
        if (requestedPosition > 0) {
            var remaining = requestedPosition
            val discardBuffer = ByteArray(8192)
            while (remaining > 0) {
                val toRead = min(remaining, discardBuffer.size.toLong()).toInt()
                val readBytes = channel.readAvailable(discardBuffer, 0, toRead)
                if (readBytes == -1) {
                    throw IOException("Reached end of stream before desired position")
                }
                remaining -= readBytes
            }
        }
    }
}