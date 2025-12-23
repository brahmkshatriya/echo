package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class ProfileAttributes(
    val data: Data
) {

    @Serializable
    data class Data(
        val me: Me
    )

    @Serializable
    data class Me(
        val profile: Profile
    )
}