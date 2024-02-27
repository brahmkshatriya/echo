package dev.brahmkshatriya.echo.common.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
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
) : Parcelable