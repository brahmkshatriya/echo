package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class CreatePlaylist(
    val revision: String,
    val uri: String
)