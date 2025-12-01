package dev.brahmkshatriya.echo.extension.spotify.models

import  kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserFollowers(
    val profiles: List<Profile>? = null
) {

    @Serializable
    data class Profile(
        val uri: String? = null,
        val name: String? = null,

        @SerialName("followers_count")
        val followersCount: Long? = null,

        val color: Long? = null,

        @SerialName("image_url")
        val imageUrl: String? = null
    )
}