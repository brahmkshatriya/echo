package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class SeedToPlaylist (
    val total: Long,
    val mediaItems: List<Item>
) {
    @Serializable
    data class Item(
        val uri: String
    )
}