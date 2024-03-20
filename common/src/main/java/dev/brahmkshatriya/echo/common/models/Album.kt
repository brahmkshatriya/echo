package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class Album {

    @Parcelize
    open class Small(
        open val id: String,
        open val title: String
    ) : Parcelable

    @Parcelize
    open class WithCover(
        override val id: String,
        override val title: String,
        open val cover: ImageHolder?,
        open val artists: List<Artist.Small>,
        open val numberOfTracks: Int?,
        open val subtitle : String?
    ) : Small(id, title)

    @Parcelize
    data class Full(
        override val id: String,
        override val title: String,
        override val cover: ImageHolder?,
        override val artists: List<Artist.Small>,
        override val numberOfTracks: Int?,
        override val subtitle: String?,
        val tracks: List<Track>,
        val releaseDate: String?,
        val publisher: String?,
        val duration: Long?,
        val description: String?,
    ) : WithCover(id, title, cover, artists, numberOfTracks, subtitle)

}
