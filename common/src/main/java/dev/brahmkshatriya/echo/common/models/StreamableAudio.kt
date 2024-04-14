package dev.brahmkshatriya.echo.common.models

import android.net.Uri
import android.os.Parcelable
import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.io.InputStream

@Parcelize
sealed class StreamableAudio : Parcelable {
    data class StreamableRequest(val request: Request) : StreamableAudio()
    data class StreamableFile(val uri: Uri) : StreamableAudio()
    data class ByteStreamAudio(val stream: @RawValue InputStream) : StreamableAudio()

    companion object {
        fun String.toAudio(headers: Map<String, String> = mapOf()) =
            StreamableRequest(this.toRequest(headers))

        fun Uri.toAudio() = StreamableFile(this)
    }
}

data class StreamableVideo(val request: Request, val looping: Boolean, val crop: Boolean)