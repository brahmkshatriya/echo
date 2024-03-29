package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Track(
    val id: String,
    val title: String,
    val streamable: Streamable? = null,
    val artists: List<Artist> = listOf(),
    val album: Album? = null,
    val cover: ImageHolder? = null,
    val duration: Long? = null,
    val plays: Int? = null,
    val releaseDate: String? = null,
    val liked: Boolean = false,
    val extras: Map<String, String> = mapOf()
) : Parcelable