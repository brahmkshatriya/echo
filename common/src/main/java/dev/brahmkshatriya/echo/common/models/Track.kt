package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

/**
 * A class representing a track that can be played in Echo.
 * 
 * @property id The id of the track
 * @property title The title of the track
 * @property artists The artists of the track
 * @property album The album of the track
 * @property cover The cover of the track
 * @property duration The duration of the track in milliseconds
 * @property plays The number of plays of the track
 * @property releaseDate The release date of the track
 * @property description The description of the track
 * @property irsc The IRSC code of the track
 * @property genres The genres of the track
 * @property albumNumber The number of the track in the album
 * @property albumDiscNumber The disc number of the track in the album
 * @property isExplicit Whether the track is explicit
 * @property subtitle The subtitle of the track, used to display information under the title
 * @property extras Any extra data you want to associate with the track
 * @property streamables The streamables of the track
 * @property isLiked Whether the track is liked
 * @property isHidden Whether the track is hidden
 * @see Artist
 * @see Album
 * @see ImageHolder
 * @see Streamable
 */
@Serializable
data class Track(
    val id: String,
    val title: String,
    val artists: List<Artist> = listOf(),
    val album: Album? = null,
    val cover: ImageHolder? = null,
    val duration: Long? = null,
    val plays: Long? = null,
    val releaseDate: Date? = null,
    val description: String? = null,
    val irsc: String? = null,
    val genres: List<String> = listOf(),
    val albumNumber: Long? = null,
    val albumDiscNumber: Long? = null,
    val isExplicit: Boolean = false,
    val subtitle: String? = null,
    val extras: Map<String, String> = mapOf(),
    val streamables: List<Streamable> = listOf(),
    val isLiked: Boolean = false,
    val isHidden: Boolean = false
) {
    /**
     * The streamable subtitles of the track.
     * @see Streamable.subtitle
     * @see Streamable.Media.Subtitle
     */
    val subtitles: List<Streamable> by lazy {
        streamables.filter { it.type == Streamable.MediaType.Subtitle }
    }

    /**
     * The streamable servers of the track.
     * @see Streamable.server
     * @see Streamable.Media.Server
     */
    val servers: List<Streamable> by lazy {
        streamables.filter { it.type == Streamable.MediaType.Server }
    }

    /**
     * The streamable backgrounds of the track.
     * @see Streamable.background
     * @see Streamable.Media.Background
     */
    val backgrounds: List<Streamable> by lazy {
        streamables.filter { it.type == Streamable.MediaType.Background }
    }
}