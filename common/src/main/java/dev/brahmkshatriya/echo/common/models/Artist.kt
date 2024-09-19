package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val cover: ImageHolder? = null,
    val followers: Int? = null,
    val description: String? = null,
    val isFollowing : Boolean = false,
    val subtitle: String? = null,
    val extras: Map<String, String> = mapOf()
)
