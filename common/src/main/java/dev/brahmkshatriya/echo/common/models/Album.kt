package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

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
    val isExplicit: Boolean = false,
    val subtitle: String? = null,
    val extras: Map<String, String> = mapOf()
)