package dev.brahmkshatriya.echo.common.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class Playlist {

    @Parcelize
    open class Small(
        open val uri: Uri,
        open val title: String,
    ) : Parcelable

    @Parcelize
    open class WithCover(
        override val uri: Uri,
        override val title: String,
        open val cover: ImageHolder?,
        val subtitle: String? = null,
    ) : Small(uri, title)

    @Parcelize
    data class Full(
        override val uri: Uri,
        override val title: String,
        override val cover: ImageHolder?,
        val author: User.Small?,
        val tracks: List<Track>,
        val creationDate: String?,
        val duration: Long?,
        val description: String?,
        val genres: List<String> = listOf(),
    ) : WithCover(uri, title, cover)
}