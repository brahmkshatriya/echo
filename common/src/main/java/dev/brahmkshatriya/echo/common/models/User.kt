package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val cover: ImageHolder? = null,
    val extras: Map<String, String> = emptyMap(),
)