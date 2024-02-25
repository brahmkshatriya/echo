package dev.brahmkshatriya.echo.common.models

import android.net.Uri

sealed class User {
    open class Small(
        open val uri: Uri,
        open val name: String
    )

    data class WithCover(
        override val uri: Uri,
        override val name: String,
        val cover: ImageHolder?,
    ) : Small(uri, name)
}