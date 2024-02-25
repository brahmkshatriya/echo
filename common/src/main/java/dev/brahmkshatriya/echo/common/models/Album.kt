package dev.brahmkshatriya.echo.common.models

import android.net.Uri

sealed class Album {

    open class Small(
        open val uri: Uri,
        open val title: String
    )

    open class WithCover(
        override val uri: Uri,
        override val title: String,
        open val cover: ImageHolder?,
        open val artists: List<Artist.Small>,
        open val numberOfTracks: Int,
    ) : Small(uri, title)

    data class Full(
        override val uri: Uri,
        override val title: String,
        override val cover: ImageHolder?,
        override val artists: List<Artist.Small>,
        override val numberOfTracks: Int,
        val tracks: List<Track>,
        val releaseDate: String?,
        val publisher: String?,
        val duration: Long?,
        val description: String?,
    ) : WithCover(uri, title, cover, artists, numberOfTracks)

}
