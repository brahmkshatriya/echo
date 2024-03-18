package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class Artist {

    @Parcelize
    open class Small(
        open val id: String,
        open val name: String
    ) : Parcelable

    @Parcelize
    open class WithCover(
        override val id: String,
        override val name: String,
        open val cover: ImageHolder?,
        open val subtitle: String? = null
    ) : Small(id, name)

    @Parcelize
    data class Full(
        override val id: String,
        override val name: String,
        override val cover: ImageHolder?,
        val description: String?,
        val followers: Int? = null,
    ) : WithCover(id, name, cover)

}