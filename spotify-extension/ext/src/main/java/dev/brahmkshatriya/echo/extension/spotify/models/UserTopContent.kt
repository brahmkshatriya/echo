package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserTopContent (
    val data: Data,
) {

    @Serializable
    data class Data(
        val me: Me
    )

    @Serializable
    data class Me(
        val profile: Profile
    )

    @Serializable
    data class Profile(
        val topArtists: PageV2? = null,
        val topTracks: PageV2? = null
    )

    @Serializable
    data class PageV2(
        @SerialName("__typename")
        val typename: String? = null,
        val items: List<Item.Wrapper>? = null,
        val totalCount: Long? = null
    )
}