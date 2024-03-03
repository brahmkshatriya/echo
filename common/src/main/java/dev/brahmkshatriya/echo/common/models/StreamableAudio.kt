package dev.brahmkshatriya.echo.common.models

import android.net.Uri
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import java.io.InputStream

sealed class StreamableAudio {
    data class StreamableUrl(val url: ImageHolder.UrlHolder) : StreamableAudio()
    data class StreamableFile(val uri: Uri) : StreamableAudio()
    data class ByteStreamAudio(val stream: InputStream) : StreamableAudio()

    companion object{
        fun String.toAudio() = StreamableUrl(this.toImageHolder())
        fun Uri.toAudio() = StreamableFile(this)
    }
}