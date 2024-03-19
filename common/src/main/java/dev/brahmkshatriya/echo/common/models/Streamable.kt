package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
open class Streamable(
    val id: String,
    val type : Type
) : Parcelable {
    @Parcelize
    enum class Type : Parcelable {
        URL,
        FILE,
        STREAM
    }
}