package dev.brahmkshatriya.echo.extension.spotify.mercury

import kotlinx.serialization.Serializable

@Serializable
data class StoredToken(
    val username: String,
    val token: String
)