package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UrlHolder(
    val url: String,
    val headers: Map<String, String> = emptyMap()
) : Parcelable {
    companion object {
        fun String.toUrlHolder(headers: Map<String, String> = emptyMap()): UrlHolder {
            return UrlHolder(this, headers)
        }
    }

}