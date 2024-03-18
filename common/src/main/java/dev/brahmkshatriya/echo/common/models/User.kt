package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class User {

    @Parcelize
    open class Small(
        open val id: String,
        open val name: String
    ) : Parcelable

    @Parcelize
    data class WithCover(
        override val id: String,
        override val name: String,
        val cover: ImageHolder?,
    ) : Small(id, name)
}