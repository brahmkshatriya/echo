package dev.brahmkshatriya.echo.data.models

import java.io.InputStream

sealed class StreamableAudio {
    data class StreamableUrl(val url: FileUrl) : StreamableAudio()
    data class ByteStreamAudio(val stream: InputStream) : StreamableAudio()
}