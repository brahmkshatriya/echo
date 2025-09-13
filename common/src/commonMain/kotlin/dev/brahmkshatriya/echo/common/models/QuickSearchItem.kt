package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Represents a quick search item.
 * This can be a [Query] or a [Media] item.
 *
 * @property searched whether the item was already searched by the user.
 */
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("quickSearchItemType")
@Serializable
sealed class QuickSearchItem {
    abstract val searched: Boolean

    /**
     * Represents a search query.
     *
     * @property query the search query.
     * @property extras additional information about the search query.
     */
    @Serializable
    data class Query(
        val query: String,
        override val searched: Boolean,
        val extras: Map<String, String> = mapOf()
    ) : QuickSearchItem()

    /**
     * Represents a media item.
     *
     * @property media the media item.
     *
     * @see EchoMediaItem
     */
    @Serializable
    data class Media(
        val media: EchoMediaItem,
        override val searched: Boolean
    ) : QuickSearchItem()

    open val title: String
        get() = when (this) {
            is Query -> query
            is Media -> media.title
        }

    fun sameAs(other: QuickSearchItem): Boolean {
        return when (this) {
            is Query -> other is Query && query == other.query
            is Media -> other is Media && media.sameAs(other.media)
        }
    }
}