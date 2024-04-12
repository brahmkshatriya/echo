package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Request(
    val url: String,
    val headers: Map<String, String> = emptyMap()
) : Parcelable {
    companion object {
        fun String.toRequest(headers: Map<String, String> = emptyMap()): Request {
            return Request(this, headers)
        }
    }

}