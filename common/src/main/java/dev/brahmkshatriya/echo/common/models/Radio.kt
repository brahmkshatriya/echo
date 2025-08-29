package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.clients.FollowClient
import dev.brahmkshatriya.echo.common.clients.HideClient
import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.clients.SaveClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import kotlinx.serialization.Serializable

/**
 * A data class representing a radio, that can load tracks and be directly played when clicked.
 *
 * @property id The id of the radio
 * @property title The title of the radio
 * @property cover The cover image of the radio
 * @property authors The authors of the radio
 * @property trackCount The number of tracks in the radio, if applicable
 * @property subtitle The subtitle of the radio, used to display information under the title
 * @property extras Any extra data you want to associate with the radio
 * @property isFollowable Whether the radio can be followed. Checkout [FollowClient]
 * @property isSaveable Whether the radio can be saved to library. Checkout [SaveClient]
 * @property isLikeable Whether the radio can be liked. Checkout [LikeClient]
 * @property isHideable Whether the radio can be hidden. Checkout [HideClient]
 * @property isShareable Whether the radio can be shared. Checkout [ShareClient]
 */
@Serializable
data class Radio(
    override val id: String,
    override val title: String,
    override val cover: ImageHolder? = null,
    val authors: List<Artist> = listOf(),
    override val trackCount: Long? = null,
    override val description: String? = null,
    override val subtitle: String? = null,
    override val extras: Map<String, String> = mapOf(),
    override val isFollowable: Boolean = false,
    override val isSaveable: Boolean = false,
    override val isLikeable: Boolean = false,
    override val isHideable: Boolean = false,
    override val isShareable: Boolean = true,
) : EchoMediaItem.Lists {
    override val isRadioSupported = false
    override val artists = authors
    override val background: ImageHolder? = null
}