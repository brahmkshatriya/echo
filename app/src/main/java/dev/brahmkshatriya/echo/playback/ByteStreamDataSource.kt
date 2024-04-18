package dev.brahmkshatriya.echo.playback

import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import dev.brahmkshatriya.echo.common.models.StreamableAudio

@OptIn(UnstableApi::class)
class ByteStreamDataSource : BaseDataSource(true) {

    class Factory : DataSource.Factory {
        override fun createDataSource() = ByteStreamDataSource()
    }


    private var audio: StreamableAudio.ByteStreamAudio? = null

    override fun read(buffer: ByteArray, offset: Int, length: Int) =
        audio!!.stream.read(buffer, offset, length)

    override fun open(dataSpec: DataSpec): Long {
        val audio = dataSpec.customData as StreamableAudio.ByteStreamAudio
        this.audio = audio
        return audio.totalBytes
    }

    override fun getUri() = audio?.hashCode().toString().toUri()
    override fun close() {
        audio?.stream?.close()
        audio = null
    }
}