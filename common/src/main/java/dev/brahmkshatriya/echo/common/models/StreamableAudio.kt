package dev.brahmkshatriya.echo.common.models

import android.net.Uri
import android.os.Parcelable
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.io.InputStream

@Parcelize
sealed class StreamableAudio : Parcelable {
    data class StreamableUrl(val url: ImageHolder.UrlHolder) : StreamableAudio()
    data class StreamableFile(val uri: Uri) : StreamableAudio()
    data class ByteStreamAudio(val stream: @RawValue InputStream) : StreamableAudio()

    companion object {
        fun String.toAudio() = StreamableUrl(this.toImageHolder(mapOf()))
        fun Uri.toAudio() = StreamableFile(this)
    }
}