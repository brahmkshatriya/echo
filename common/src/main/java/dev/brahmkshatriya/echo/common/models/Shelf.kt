package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.helpers.PagedData
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Represents a shelf (group of Media Items or Categories) in the app feed.
 *
 * This can be [Lists] shelf, an [Item] shelf or a [Category] shelf.
 *
 * @see Lists
 * @see Item
 * @see Category
 */
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("shelfType")
@Serializable
sealed interface Shelf {
    val id: String
    val title: String
    val extras: Map<String, String>

    /**
     * Represents a list of media items or categories.
     * If [more] is not null, a "More" button will be displayed
     * and clicking on it will load a separate page for loading the [PagedData].
     *
     * Can be a list of [Items], [Tracks] or [Categories].
     *
     * @property title the title of the list.
     * @property list the list of media items or categories.
     * @property subtitle the subtitle of the list.
     * @property type the type of the list.
     * @property more the more data of the list.
     *
     * @see Items
     * @see Tracks
     * @see Categories
     */
    @OptIn(ExperimentalSerializationApi::class)
    @JsonClassDiscriminator("shelfType")
    @Serializable
    sealed interface Lists<T : Any> : Shelf {
        val list: List<T>
        val subtitle: String?
        val type: Type
        val more: Feed<Shelf>?

        /**
         * Represents the type of the list.
         *
         * - [Type.Linear] items displayed in a horizontally.
         * - [Type.Grid] items displayed in a grid layout, vertically. limited to 8 items.
         */
        enum class Type {
            Linear, Grid
        }

        /**
         * Represents a list of [EchoMediaItem].
         *
         * @property id the unique identifier of the list.
         * @property title the title of the list.
         * @property list the list of media items.
         * @property subtitle the subtitle of the list.
         * @property type the type of the list.
         * @property more the more data of the list.
         */
        @Serializable
        data class Items(
            override val id: String,
            override val title: String,
            override val list: List<EchoMediaItem>,
            override val subtitle: String? = null,
            override val type: Type = Type.Linear,
            @Transient override val more: Feed<Shelf>? = null,
            override val extras: Map<String, String> = mapOf()
        ) : Lists<EchoMediaItem>

        /**
         * Represents a list of [Track], these will be numbered, otherwise use [Items] instead.
         *
         * @property id the unique identifier of the list.
         * @property title the title of the list.
         * @property list the list of tracks.
         * @property subtitle the subtitle of the list.
         * @property type the type of the list.
         * @property more the more data of the list.
         */
        @Serializable
        data class Tracks(
            override val id: String,
            override val title: String,
            override val list: List<Track>,
            override val subtitle: String? = null,
            override val type: Type = Type.Linear,
            @Transient override val more: Feed<Shelf>? = null,
            override val extras: Map<String, String> = mapOf()
        ) : Lists<Track>

        /**
         * Represents a list of [Category].
         *
         * @property id the unique identifier of the list.
         * @property title the title of the list.
         * @property list the list of categories.
         * @property subtitle the subtitle of the list.
         * @property type the type of the list.
         * @property more the more data of the list.
         */
        @Serializable
        data class Categories(
            override val id: String,
            override val title: String,
            override val list: List<Category>,
            override val subtitle: String? = null,
            override val type: Type = Type.Linear,
            @Transient override val more: Feed<Shelf>? = null,
            override val extras: Map<String, String> = mapOf()
        ) : Lists<Category>
    }

    /**
     * Represents a media item.
     *
     * @property media the media item.
     */
    @Serializable
    data class Item(
        val media: EchoMediaItem
    ) : Shelf {
        override val id = media.id
        override val title = media.title
        override val extras = media.extras
    }

    /**
     * Represents a category of media items.
     *
     * If [feed] is not null, clicking on this will load a separate page for loading the [Feed].
     * If [feed] is null, the category will act as a header.
     *
     * @property id the unique identifier of the category.
     * @property title the title of the category.
     * @property feed the items of the category.
     * @property subtitle the subtitle of the category.
     * @property image the image of the category.
     * @property backgroundColor the background color in hex. (#RRGGBB & #AARRGGBB)
     * @property extras additional information about the category.
     */
    @Serializable
    data class Category(
        override val id: String,
        override val title: String,
        @Transient val feed: Feed<Shelf>? = null,
        val subtitle: String? = null,
        val image: ImageHolder? = null,
        val backgroundColor: String? = null,
        override val extras: Map<String, String> = mapOf(),
    ) : Shelf
}