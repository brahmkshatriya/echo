package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import kotlinx.serialization.Serializable
import java.io.InputStream

@Serializable
sealed class StreamableAudio {
    data class StreamableRequest(val request: Request) : StreamableAudio()
    data class ByteStreamAudio(val stream: InputStream, val totalBytes: Long) :
        StreamableAudio()

    companion object {
        fun String.toAudio(headers: Map<String, String> = mapOf()) =
            StreamableRequest(this.toRequest(headers))
    }
}