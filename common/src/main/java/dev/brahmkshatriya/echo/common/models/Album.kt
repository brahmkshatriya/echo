package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

/**
 * A data class representing an album
 *
 * @property id The id of the album
 * @property title The title of the album
 * @property cover The cover image of the album
 * @property artists The artists of the album
 * @property duration The duration of the album in milliseconds
 * @property tracks The total number of tracks in the album
 * @property releaseDate The release date of the album
 * @property label The publisher of the album
 * @property description The description of the album
 * @property isExplicit Whether the album is explicit
 * @property subtitle The subtitle of the album, used to display information under the title
 * @property extras Any extra data you want to associate with the album
 */
@Serializable
data class Album(
    val id: String,
    val title: String,
    val cover: ImageHolder? = null,
    val artists: List<Artist> = listOf(),
    val tracks: Int? = null,
    val duration: Long? = null,
    val releaseDate: Date? = null,
    val description: String? = null,
    val label: String? = null,
    val isExplicit: Boolean = false,
    val subtitle: String? = null,
    val extras: Map<String, String> = mapOf()
)