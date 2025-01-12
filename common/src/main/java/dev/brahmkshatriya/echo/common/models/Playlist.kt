package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

/**
 * A data class representing a playlist
 *
 * @property id The id of the playlist
 * @property title The title of the playlist
 * @property isEditable Whether the playlist is editable
 * @property cover The cover image of the playlist
 * @property authors The authors of the playlist
 * @property tracks The total number of tracks in the playlist
 * @property duration The total duration of the playlist in milliseconds
 * @property creationDate The creation date of the playlist
 * @property description The description of the playlist
 * @property subtitle The subtitle of the playlist, used to display information under the title
 * @property isPrivate Whether the playlist is private
 * @property extras Any extra data you want to associate with the playlist
 */
@Serializable
data class Playlist(
    val id: String,
    val title: String,
    val isEditable: Boolean,
    val cover: ImageHolder? = null,
    val authors: List<User> = listOf(),
    val tracks: Int? = null,
    val duration: Long? = null,
    val creationDate: Date? = null,
    val description: String? = null,
    val subtitle: String? = null,
    val isPrivate: Boolean = true,
    val extras: Map<String, String> = mapOf(),
)