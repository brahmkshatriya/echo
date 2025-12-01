package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class GetTrack(
    val data: Data
){
    @Serializable
    data class Data(
        val trackUnion: Item.Track
    )
}
