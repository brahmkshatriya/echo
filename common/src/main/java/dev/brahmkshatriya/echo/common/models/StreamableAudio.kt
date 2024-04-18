package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.io.InputStream

@Parcelize
sealed class StreamableAudio : Parcelable {
    data class StreamableRequest(val request: Request) : StreamableAudio()
    data class ByteStreamAudio(val stream: @RawValue InputStream, val totalBytes: Long) :
        StreamableAudio()

    companion object {
        fun String.toAudio(headers: Map<String, String> = mapOf()) =
            StreamableRequest(this.toRequest(headers))
    }
}

data class StreamableVideo(val request: Request, val looping: Boolean, val crop: Boolean)