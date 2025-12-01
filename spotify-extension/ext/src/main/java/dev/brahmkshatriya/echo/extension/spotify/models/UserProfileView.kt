package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileView (
    val uri: String? = null,
    val name: String? = null,

    @SerialName("image_url")
    val imageUrl: String? = null,

    @SerialName("followers_count")
    val followersCount: Long? = null,

    @SerialName("following_count")
    val followingCount: Long? = null,

    @SerialName("public_playlists")
    val publicPlaylists: List<PublicPlaylist>? = null,

    @SerialName("total_public_playlists_count")
    val totalPublicPlaylistsCount: Long? = null,

    @SerialName("has_spotify_name")
    val hasSpotifyName: Boolean? = null,

    @SerialName("has_spotify_image")
    val hasSpotifyImage: Boolean? = null,

    val color: Long? = null,

    @SerialName("allow_follows")
    val allowFollows: Boolean? = null,

    @SerialName("show_follows")
    val showFollows: Boolean? = null
) {

    @Serializable
    data class PublicPlaylist(
        val uri: String? = null,
        val name: String? = null,

        @SerialName("image_url")
        val imageUrl: String? = null,

        @SerialName("owner_name")
        val ownerName: String? = null,

        @SerialName("owner_uri")
        val ownerUri: String? = null,

        @SerialName("followers_count")
        val followersCount: Long? = null
    )
}
