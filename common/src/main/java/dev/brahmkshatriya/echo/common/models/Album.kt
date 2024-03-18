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
        open val artist: Artist.Small,
        open val numberOfTracks: Int,
    ) : Small(id, title)

    @Parcelize
    data class Full(
        override val id: String,
        override val title: String,
        override val cover: ImageHolder?,
        override val artist: Artist.Small,
        override val numberOfTracks: Int,
        val tracks: List<Track>,
        val releaseDate: String?,
        val publisher: String?,
        val duration: Long?,
        val description: String?,
    ) : WithCover(id, title, cover, artist, numberOfTracks)

}
