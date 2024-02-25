package dev.brahmkshatriya.echo.common.models

import android.net.Uri

sealed class Artist {

    open class Small(
        open val uri: Uri,
        open val name: String
    )

    open class WithCover(
        override val uri: Uri,
        override val name: String,
        open val cover: ImageHolder?,
        open val subtitle: String? = null
    ) : Small(uri, name)

    data class Full(
        override val uri: Uri,
        override val name: String,
        override val cover: ImageHolder?,
        val description: String?,
        val followers: Int? = null,
    ) : WithCover(uri, name, cover)

}