package dev.brahmkshatriya.echo.common.models

import android.net.Uri
import android.os.Parcelable
import dev.brahmkshatriya.echo.common.models.UrlHolder.Companion.toUrlHolder
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.io.InputStream

@Parcelize
sealed class StreamableAudio : Parcelable {
    data class StreamableUrl(val urlHolder: UrlHolder) : StreamableAudio()
    data class StreamableFile(val uri: Uri) : StreamableAudio()
    data class ByteStreamAudio(val stream: @RawValue InputStream) : StreamableAudio()

    companion object {
        fun String.toAudio(headers: Map<String, String> = mapOf()) =
            StreamableUrl(this.toUrlHolder(headers))

        fun Uri.toAudio() = StreamableFile(this)
    }
}