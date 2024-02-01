package dev.brahmkshatriya.echo.data.models

import android.net.Uri

sealed class Album {

    open class Small(
        open val uri: Uri,
        open val title: String
    )

    open class WithCover(
        override val uri: Uri,
        override val title: String,
        open val cover: FileUrl?
    ) : Small(uri, title)

    data class Full(
        override val uri: Uri,
        override val title: String,
        override val cover: FileUrl?,
        val artists: List<Artist.Small>,
        val tracks: List<Track.Small>,
        val releaseDate: String?,
        val publisher: String?,
        val duration: Long?,
        val description: String?,
    ) : WithCover(uri, title, cover)

}
