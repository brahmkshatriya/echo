package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import java.io.InputStream

import kotlinx.serialization.Serializable
import java.io.Serializable as JSerializable

@Serializable
sealed class StreamableAudio : JSerializable {
    data class StreamableRequest(val request: Request) : StreamableAudio()
    data class ByteStreamAudio(val stream: InputStream, val totalBytes: Long) :
        StreamableAudio()

    companion object {
        fun String.toAudio(headers: Map<String, String> = mapOf()) =
            StreamableRequest(this.toRequest(headers))
    }
}