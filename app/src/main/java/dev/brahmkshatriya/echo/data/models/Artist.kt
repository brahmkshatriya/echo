package dev.brahmkshatriya.echo.data.models

import android.net.Uri

sealed class Artist {

    open class Small(
        open val uri: Uri,
        open val name: String
    )

    open class WithCover(
        override val uri: Uri,
        override val name: String,
        open val cover: FileUrl?
    ) : Small(uri, name)

    data class Full(
        override val uri: Uri,
        override val name: String,
        override val cover: FileUrl?,
        val description: String?,
        val followers: Int? = null,
    ) : WithCover(uri, name, cover)

}