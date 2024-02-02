package dev.brahmkshatriya.echo.data.models

import java.io.InputStream

sealed class StreamableAudio {
    data class StreamableUrl(val url: ImageHolder) : StreamableAudio()
    data class ByteStreamAudio(val stream: InputStream) : StreamableAudio()
}