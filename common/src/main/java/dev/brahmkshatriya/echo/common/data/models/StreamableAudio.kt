package dev.brahmkshatriya.echo.common.data.models

import android.net.Uri
import dev.brahmkshatriya.echo.common.data.models.ImageHolder.Companion.toImageHolder
import java.io.InputStream

sealed class StreamableAudio {
    data class StreamableUrl(val url: ImageHolder.UrlHolder) : StreamableAudio()
    data class StreamableFile(val uri: Uri) : StreamableAudio()
    data class ByteStreamAudio(val stream: InputStream) : StreamableAudio()

    companion object{
        fun String?.toAudio() = this?.let { StreamableUrl(it.toImageHolder()) }
        fun Uri?.toAudio() = this?.let { StreamableFile(it) }
    }
}