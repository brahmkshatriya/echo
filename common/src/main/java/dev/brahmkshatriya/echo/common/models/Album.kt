package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable
import java.io.Serializable as JSerializable

@Serializable
data class Album(
    val id: String,
    val title: String,
    val cover: ImageHolder? = null,
    val artists: List<Artist> = listOf(),
    val tracks: Int? = null,
    val releaseDate: String? = null,
    val publisher: String? = null,
    val duration: Long? = null,
    val description: String? = null,
    val subtitle: String? = null,
    val extras: Map<String, String> = mapOf()
) : JSerializable