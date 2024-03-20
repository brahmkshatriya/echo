package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class Playlist {

    @Parcelize
    open class Small(
        open val id: String,
        open val title: String,
    ) : Parcelable

    @Parcelize
    open class WithCover(
        override val id: String,
        override val title: String,
        open val cover: ImageHolder?,
        val subtitle: String? = null,
    ) : Small(id, title)

    @Parcelize
    data class Full(
        override val id: String,
        override val title: String,
        override val cover: ImageHolder?,
        val authors: List<User.Small>,
        val tracks: List<Track>,
        val creationDate: String?,
        val duration: Long?,
        val description: String?,
        val genres: List<String> = listOf(),
    ) : WithCover(id, title, cover)
}