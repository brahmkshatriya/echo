package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class ArtistOverview(
    val data: Data
){
    @Serializable
    data class Data(
        val artistUnion: Item.Artist
    )
}
