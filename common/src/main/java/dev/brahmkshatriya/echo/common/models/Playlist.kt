package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: String,
    val title: String,
    val isEditable: Boolean,
    val cover: ImageHolder? = null,
    val authors: List<User> = listOf(),
    val tracks: Int? = null,
    val duration: Long? = null,
    val creationDate: String? = null,
    val description: String? = null,
    val subtitle: String? = null,
    val isPrivate: Boolean = true,
    val extras: Map<String, String> = mapOf(),
)