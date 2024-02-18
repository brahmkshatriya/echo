package dev.brahmkshatriya.echo.common.data.models

import android.net.Uri

sealed class User {
    open class Small(
        open val uri: Uri,
        open val name: String
    )

    open class WithCover(
        override val uri: Uri,
        override val name: String,
        open val cover: ImageHolder?,
    ) : Small(uri, name)
}