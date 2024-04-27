package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Playlist(
    val id: String,
    val title: String,
    val isEditable: Boolean,
    val cover: ImageHolder? = null,
    val authors: List<User> = listOf(),
    val tracks: List<Track> = listOf(),
    val creationDate: String? = null,
    val duration: Long? = null,
    val description: String? = null,
    val subtitle: String? = null,
    val extras: Map<String, String> = mapOf()
) : Parcelable