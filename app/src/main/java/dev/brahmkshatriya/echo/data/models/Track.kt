package dev.brahmkshatriya.echo.data.models

import android.net.Uri

sealed class Track {
    open class Small(
        open val uri: Uri,
        open val title: String,
        open val artists: List<Artist.Small> = listOf(),
        open val album: Album.Small?,
        open val cover: ImageHolder?,
        open val duration: Long?,
        open val plays: Int?,
        open val releaseDate: String?,
        open val liked: Boolean,
    )

    data class Full(
        override val uri: Uri,
        override val title: String,
        override val artists: List<Artist.Small> = listOf(),
        override val album: Album.Small?,
        override val cover: ImageHolder?,
        override val duration: Long?,
        override val plays: Int?,
        override val releaseDate: String?,
        override val liked: Boolean,
        val genres: List<String> = listOf(),
        val description: String?,
    ) : Small(uri, title, artists, album, cover, duration, plays, releaseDate, liked)
}