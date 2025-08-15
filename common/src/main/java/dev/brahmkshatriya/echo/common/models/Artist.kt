package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.FollowClient
import dev.brahmkshatriya.echo.common.clients.HideClient
import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SaveClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import kotlinx.serialization.Serializable

/**
 * A data class representing an artist
 *
 * @property id The id of the artist
 * @property name The name of the artist
 * @property cover The cover image of the artist
 * @property bio The bio of the artist
 * @property background The background image of the artist
 * @property banners The banners of the artist (not used yet)
 * @property subtitle The subtitle of the artist, used to display information under the name
 * @property extras Any extra data you want to associate with the artist
 * @property isRadioSupported Whether the artist can be used to create a radio. Checkout [RadioClient]
 * @property isFollowable Whether the artist can be followed. Checkout [FollowClient]
 * @property isSaveable Whether the artist can be saved to library. Checkout [SaveClient]
 * @property isLikeable Whether the artist can be liked. Checkout [LikeClient]
 * @property isHideable Whether the artist can be hidden. Checkout [HideClient]
 * @property isShareable Whether the artist can be shared. Checkout [ShareClient]
 *
 * @see ArtistClient
 */
@Serializable
data class Artist(
    override val id: String,
    val name: String,
    override val cover: ImageHolder? = null,
    val bio: String? = null,
    override val background: ImageHolder? = cover,
    val banners: List<ImageHolder> = listOf(),
    override val subtitle: String? = null,
    override val extras: Map<String, String> = mapOf(),
    override val isRadioSupported: Boolean = true,
    override val isFollowable: Boolean = true,
    override val isSaveable: Boolean = true,
    override val isLikeable: Boolean = false,
    override val isHideable: Boolean = false,
    override val isShareable: Boolean = true,
) : EchoMediaItem {
    override val title = name
    override val description = bio
    override val subtitleWithOutE = subtitle
}
