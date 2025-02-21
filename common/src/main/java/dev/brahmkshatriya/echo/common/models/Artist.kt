package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

/**
 * A data class representing an artist
 *
 * @property id The id of the artist
 * @property name The name of the artist
 * @property cover The cover image of the artist
 * @property followers The number of followers the artist has
 * @property description The description of the artist
 * @property banners The banners of the artist (not used yet)
 * @property isFollowing Whether the user is following the artist
 * @property subtitle The subtitle of the artist, used to display information under the name
 * @property extras Any extra data you want to associate with the artist
 */
@Serializable
data class Artist(
    val id: String,
    val name: String,
    val cover: ImageHolder? = null,
    val followers: Int? = null,
    val description: String? = null,
    val banners: List<ImageHolder> = listOf(),
    val isFollowing: Boolean = false,
    val subtitle: String? = null,
    val extras: Map<String, String> = mapOf()
)
