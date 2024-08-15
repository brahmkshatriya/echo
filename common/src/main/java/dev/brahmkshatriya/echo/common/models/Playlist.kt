package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable
import java.io.Serializable as JSerializable

@Serializable
data class Playlist(
    val id: String,
    val title: String,
    val isEditable: Boolean,
    val cover: ImageHolder? = null,
    val authors: List<User> = listOf(),
    val tracks: Int? = null,
    val creationDate: String? = null,
    val duration: Long? = null,
    val description: String? = null,
    val subtitle: String? = null,
    val extras: Map<String, String> = mapOf()
) : JSerializable