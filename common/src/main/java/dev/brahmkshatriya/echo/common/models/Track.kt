package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.clients.FollowClient
import dev.brahmkshatriya.echo.common.clients.HideClient
import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SaveClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * A class representing a track that can be played in Echo.
 *
 * @property id The id of the track
 * @property title The title of the track
 * @property type The type of the track, can be Audio, Video, or HorizontalVideo
 * @property cover The cover of the track
 * @property artists The artists of the track
 * @property album The album of the track
 * @property duration The duration of the track in milliseconds
 * @property playedDuration The duration of the track that has been played, in milliseconds
 * @property plays The number of plays of the track
 * @property releaseDate The release date of the track
 * @property description The description of the track
 * @property background The background image of the track
 * @property isrc The IRSC code of the track
 * @property genres The genres of the track
 * @property albumDiscNumber The disc number of the track in the album
 * @property albumOrderNumber The order number of the track in the album
 * @property playlistAddedDate The date when the track was added to a playlist
 * @property isExplicit Whether the track is explicit
 * @property subtitle The subtitle of the track, used to display information under the title
 * @property extras Any extra data you want to associate with the track
 * @property isPlayable Whether the track is playable.
 * @property streamables The streamables of the track
 * @property isRadioSupported Whether the track can used to create a radio. Checkout [RadioClient]
 * @property isFollowable Whether the track can be followed. Checkout [FollowClient]
 * @property isSaveable Whether the track can be saved to library. Checkout [SaveClient]
 * @property isLikeable Whether the track can be liked. Checkout [LikeClient]
 * @property isHideable Whether the track can be hidden. Checkout [HideClient]
 * @property isShareable Whether the track can be shared. Checkout [ShareClient]
 *
 * @see Streamable
 * @see TrackClient
 */
@Serializable
data class Track(
    override val id: String,
    override val title: String,
    val type: Type = Type.Song,
    override val cover: ImageHolder? = null,
    val artists: List<Artist> = listOf(),
    val album: Album? = null,
    val duration: Long? = null,
    val playedDuration: Long? = null,
    val plays: Long? = null,
    val releaseDate: Date? = null,
    override val description: String? = null,
    override val background: ImageHolder? = cover,
    val genres: List<String> = listOf(),
    val isrc: String? = null,
    val albumOrderNumber: Long? = null,
    val albumDiscNumber: Long? = null,
    val playlistAddedDate: Date? = null,
    override val isExplicit: Boolean = false,
    override val subtitle: String? = null,
    override val extras: Map<String, String> = mapOf(),
    val isPlayable: Playable = Playable.Yes,
    val streamables: List<Streamable> = listOf(),
    override val isRadioSupported: Boolean = true,
    override val isFollowable: Boolean = false,
    override val isSaveable: Boolean = true,
    override val isLikeable: Boolean = true,
    override val isHideable: Boolean = true,
    override val isShareable: Boolean = true,
) : EchoMediaItem {

    enum class Type {
        Song, Podcast, VideoSong, Video, HorizontalVideo
    }

    @Serializable
    sealed interface Playable {
        @Serializable
        data object Yes : Playable

        @Serializable
        data object RegionLocked : Playable

        @Serializable
        data object Unreleased : Playable

        @Serializable
        data class No(val reason: String) : Playable
    }

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

    override val subtitleWithOutE = subtitle ?: buildString {
        if (duration != null) append(duration.toDurationString())
        val artists = artists.joinToString(", ") { it.name }
        if (artists.isNotBlank()) {
            if (duration != null) append(" • ")
            append(artists)
        }
    }.trim().ifBlank { null }

    override val subtitleWithE = buildString {
        if (isExplicit) append("\uD83C\uDD74 ")
        append(subtitleWithOutE ?: "")
    }.trim().ifBlank { null }

    companion object {
        fun Long.toDurationString(): String {
            val seconds = this / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            return buildString {
                if (hours > 0) append(String.format(Locale.getDefault(), "%02d:", hours))
                append(String.format(Locale.getDefault(), "%02d:%02d", minutes % 60, seconds % 60))
            }.trim()
        }
    }
}