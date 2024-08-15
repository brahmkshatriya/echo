package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val cover: ImageHolder? = null,
    val extras: Map<String, String> = mapOf(),
    val subtitle: String? = null,
    val description: String? = null,
    val followers: Int? = null,
    val isFollowing : Boolean = false,
)
