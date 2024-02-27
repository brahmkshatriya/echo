package dev.brahmkshatriya.echo.common.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class User {

    @Parcelize
    open class Small(
        open val uri: Uri,
        open val name: String
    ) : Parcelable

    @Parcelize
    data class WithCover(
        override val uri: Uri,
        override val name: String,
        val cover: ImageHolder?,
    ) : Small(uri, name)
}