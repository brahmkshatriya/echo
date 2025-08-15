package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.clients.FollowClient
import dev.brahmkshatriya.echo.common.clients.HideClient
import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SaveClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * A class representing a media item in Echo.
 *
 * Use [toShelf] to convert a media item to [Shelf.Item].
 *
 * Example:
 * ```kotlin
 * val track = api.getTrack("track_id")
 * val shelfItem = mediaItem.toShelf()
 * ```
 * @property id The id of the media item
 * @property title The title of the media item
 * @property cover The cover image of the media item
 * @property description A description of the media item, used to display additional information
 * @property background The background image of the media item, used to display a background image
 * @property subtitle The subtitle of the media item, used to display information under the title
 * @property extras Any extra data you want to associate with the media item
 * @property isFollowable Whether the media item can be followed. Checkout [FollowClient]
 * @property isSaveable Whether the media item can be saved to library. Checkout [SaveClient]
 * @property isLikeable Whether the media item can be liked. Checkout [LikeClient]
 * @property isHideable Whether the media item can be hidden. Checkout [HideClient]
 * @property isRadioSupported Whether the media item can be loaded to get [Radio]. Checkout [RadioClient]
 * @property isShareable Whether the media item can be shared. Checkout [ShareClient]
 *
 * @see Track
 * @see Artist
 * @see User
 * @see Album
 * @see Playlist
 * @see Radio
 */
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("mediaItemType")
@Serializable
sealed interface EchoMediaItem {
    val id: String
    val title: String
    val cover: ImageHolder?
    val description: String?
    val background: ImageHolder?
    val subtitle: String?
    val extras: Map<String, String>
    val isRadioSupported: Boolean
    val isFollowable: Boolean
    val isSaveable: Boolean
    val isLikeable: Boolean
    val isHideable: Boolean
    val isShareable: Boolean
    val isExplicit: Boolean get() = false
    val isPrivate: Boolean get() = false

    @Serializable
    sealed interface Lists : EchoMediaItem {
        val artists: List<Artist>
        val trackCount: Long?
        val duration: Long? get() = null
        val date: Date? get() = null
        val label: String? get() = null
        val type: Album.Type? get() = null

        override val subtitleWithOutE
            get() = subtitle ?: buildString {
                append(artists.joinToString(", ") { it.name })
            }.trim().ifBlank { null }
    }


    val subtitleWithOutE: String?

    val subtitleWithE
        get() = buildString {
            if (isExplicit) append("\uD83C\uDD74 ")
            append(subtitleWithOutE ?: "")
        }.trim().ifBlank { null }

    fun sameAs(other: EchoMediaItem): Boolean {
        return this::class == other::class && id == other.id
    }

    fun toShelf() = Shelf.Item(this)

    fun copyMediaItem(
        id: String = this.id,
        title: String = this.title,
        cover: ImageHolder? = this.cover,
        description: String? = this.description,
        subtitle: String? = this.subtitle,
        extras: Map<String, String> = this.extras,
        isRadioSupported: Boolean = this.isRadioSupported,
        isFollowable: Boolean = this.isFollowable,
        isSaveable: Boolean = this.isSaveable
    ): EchoMediaItem = when (this) {
        is Artist -> copy(
            id = id,
            name = title,
            cover = cover,
            bio = description,
            subtitle = subtitle,
            extras = extras,
            isRadioSupported = isRadioSupported,
            isFollowable = isFollowable,
            isSaveable = isSaveable
        )

        is Album -> copy(
            id = id,
            title = title,
            cover = cover,
            description = description,
            subtitle = subtitle,
            extras = extras,
            isRadioSupported = isRadioSupported,
            isFollowable = isFollowable,
            isSaveable = isSaveable
        )

        is Playlist -> copy(
            id = id,
            title = title,
            cover = cover,
            description = description,
            subtitle = subtitle,
            extras = extras,
            isRadioSupported = isRadioSupported,
            isFollowable = isFollowable,
            isSaveable = isSaveable
        )

        is Radio -> copy(
            id = id,
            title = title,
            cover = cover,
            description = description,
            subtitle = subtitle,
            extras = extras,
            isFollowable = isFollowable,
            isSaveable = isSaveable
        )

        is Track -> copy(
            id = id,
            title = title,
            cover = cover,
            description = description,
            subtitle = subtitle,
            extras = extras,
            isRadioSupported = isRadioSupported,
            isFollowable = isFollowable,
            isSaveable = isSaveable
        )
    }
}