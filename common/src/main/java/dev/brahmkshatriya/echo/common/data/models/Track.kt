package dev.brahmkshatriya.echo.common.data.models

import android.net.Uri

data class Track(
    val uri: Uri,
    val title: String,
    val artists: List<Artist.Small> = listOf(),
    val album: Album.Small?,
    val cover: ImageHolder?,
    val duration: Long?,
    val plays: Int?,
    val releaseDate: String?,
    val liked: Boolean,
)