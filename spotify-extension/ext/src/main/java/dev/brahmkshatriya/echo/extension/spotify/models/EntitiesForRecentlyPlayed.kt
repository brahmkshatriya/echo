package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class EntitiesForRecentlyPlayed(
    val data: Data
){
    @Serializable
    data class Data(
        val lookup: List<Item.Wrapper>
    )
}
