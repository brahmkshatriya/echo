package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class GetAlbum(
    val data: Data
) {
    @Serializable
    data class Data(
        val albumUnion: Item.Album
    )
}