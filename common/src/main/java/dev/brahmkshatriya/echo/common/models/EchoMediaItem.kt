package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.clients.FollowClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SaveToLibraryClient
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
 * @property isSavable Whether the media item can be saved to a library. Checkout [SaveToLibraryClient]
 * @property isRadioSupported Whether the media item can be loaded to get [Radio]. Checkout [RadioClient]
 * @property isSharable Whether the media item can be shared. Checkout [ShareClient]
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
    val isSavable: Boolean
    val isSharable: Boolean
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
        isSavable: Boolean = this.isSavable
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
            isSavable = isSavable
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
            isSavable = isSavable
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
            isSavable = isSavable
        )

        is Radio -> copy(
            id = id,
            title = title,
            cover = cover,
            description = description,
            subtitle = subtitle,
            extras = extras,
            isFollowable = isFollowable,
            isSavable = isSavable
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
            isSavable = isSavable
        )
    }
}