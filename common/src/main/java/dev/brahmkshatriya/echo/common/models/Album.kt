package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.FollowClient
import dev.brahmkshatriya.echo.common.clients.HideClient
import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SaveClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.models.Album.Type.Book
import dev.brahmkshatriya.echo.common.models.Album.Type.Compilation
import dev.brahmkshatriya.echo.common.models.Album.Type.EP
import dev.brahmkshatriya.echo.common.models.Album.Type.LP
import dev.brahmkshatriya.echo.common.models.Album.Type.PreRelease
import dev.brahmkshatriya.echo.common.models.Album.Type.Show
import dev.brahmkshatriya.echo.common.models.Album.Type.Single
import kotlinx.serialization.Serializable

/**
 * A data class representing an album
 *
 * @property id The id of the album
 * @property title The title of the album
 * @property type The [Type] of the album.
 * @property cover The cover image of the album
 * @property artists The artists of the album
 * @property duration The duration of the album in milliseconds
 * @property trackCount The total number of tracks in the album
 * @property releaseDate The release date of the album
 * @property label The publisher of the album
 * @property description The description of the album
 * @property background The background image of the album
 * @property isExplicit Whether the album is explicit
 * @property subtitle The subtitle of the album, used to display information under the title
 * @property extras Any extra data you want to associate with the album
 * @property isRadioSupported Whether the album can be used to create a radio. Checkout [RadioClient]
 * @property isFollowable Whether the album can be followed. Checkout [FollowClient]
 * @property isSaveable Whether the album can be saved to library. Checkout [SaveClient]
 * @property isLikeable Whether the album can be liked. Checkout [LikeClient]
 * @property isHideable Whether the album can be hidden. Checkout [HideClient]
 * @property isShareable Whether the album can be shared. Checkout [ShareClient]
 *
 * @see AlbumClient
 */
@Serializable
data class Album(
    override val id: String,
    override val title: String,
    override val type: Type? = null,
    override val cover: ImageHolder? = null,
    override val artists: List<Artist> = listOf(),
    override val trackCount: Long? = null,
    override val duration: Long? = null,
    val releaseDate: Date? = null,
    override val description: String? = null,
    override val background: ImageHolder? = cover,
    override val label: String? = null,
    override val isExplicit: Boolean = false,
    override val subtitle: String? = null,
    override val extras: Map<String, String> = mapOf(),
    override val isRadioSupported: Boolean = true,
    override val isFollowable: Boolean = false,
    override val isSaveable: Boolean = true,
    override val isLikeable: Boolean = false,
    override val isHideable: Boolean = false,
    override val isShareable: Boolean = true,
) : EchoMediaItem.Lists {

    override val date = releaseDate

    override val subtitleWithOutE = subtitle ?: buildString {
        append(artists.joinToString(", ") { it.name })
    }.trim().ifBlank { null }

    /**
     * The type of the album.
     * This can be actual album types like [PreRelease], [Single], [EP], [LP], [Compilation]
     * or Special types like :
     * - [Show] - Will represent the tracks as "Episodes"
     * - [Book] - Will represent the tracks as "Chapters"
     *
     * Other types will be represented as "Songs"
     */
    enum class Type {
        PreRelease, Single, EP, LP, Compilation, Show, Book
    }
}