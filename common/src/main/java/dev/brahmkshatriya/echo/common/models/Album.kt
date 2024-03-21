package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Album(
    val id: String,
    val title: String,
    val cover: ImageHolder? = null,
    val artists: List<Artist> = listOf(),
    val numberOfTracks: Int? = null,
    val subtitle: String? = null,
    val tracks: List<Track> = listOf(),
    val releaseDate: String? = null,
    val publisher: String? = null,
    val duration: Long? = null,
    val description: String? = null,
    val extras: Map<String, String> = mapOf()
) : Parcelable