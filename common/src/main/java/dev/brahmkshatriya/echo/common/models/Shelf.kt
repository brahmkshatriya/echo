package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf.Category
import dev.brahmkshatriya.echo.common.models.Shelf.Item
import dev.brahmkshatriya.echo.common.models.Shelf.Lists
import dev.brahmkshatriya.echo.common.models.Shelf.Lists.Categories
import dev.brahmkshatriya.echo.common.models.Shelf.Lists.Items
import dev.brahmkshatriya.echo.common.models.Shelf.Lists.Tracks

/**
 * Represents a shelf (group of Media Items or Categories) in the app feed.
 *
 * This can be [Lists] shelf, an [Item] shelf or a [Category] shelf.
 *
 * @see Lists
 * @see Item
 * @see Category
 */
sealed class Shelf {

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
    sealed class Lists<T : Any>(
        open val title: String,
        open val list: List<T>,
        open val subtitle: String?,
        open val type: Type,
        open val more: PagedData<T>?
    ) : Shelf() {

        /**
         * Represents the type of the list.
         *
         * If [Type.Linear], the list will be displayed in a horizontal linear layout.
         * If [Type.Grid], the list will be displayed in a grid layout.
         */
        enum class Type {
            Linear, Grid
        }

        /**
         * Represents a list of [EchoMediaItem].
         *
         * @property title the title of the list.
         * @property list the list of media items.
         * @property subtitle the subtitle of the list.
         * @property type the type of the list.
         * @property more the more data of the list.
         */
        data class Items(
            override val title: String,
            override val list: List<EchoMediaItem>,
            override val subtitle: String? = null,
            override val type: Type = Type.Linear,
            override val more: PagedData<EchoMediaItem>? = null
        ) : Lists<EchoMediaItem>(
            title = title,
            list = list,
            subtitle = subtitle,
            type = type,
            more = more
        )

        /**
         * Represents a list of [Track].
         * If [isNumbered] is true, the tracks will be numbered and
         * clicking on any track will load the entire list into the playing queue.
         *
         * @property title the title of the list.
         * @property list the list of tracks.
         * @property subtitle the subtitle of the list.
         * @property type the type of the list.
         * @property isNumbered whether the tracks are numbered.
         * @property more the more data of the list.
         */
        data class Tracks(
            override val title: String,
            override val list: List<Track>,
            override val subtitle: String? = null,
            override val type: Type = Type.Linear,
            val isNumbered: Boolean = false,
            override val more: PagedData<Track>? = null
        ) : Lists<Track>(
            title = title,
            list = list,
            type = type,
            subtitle = subtitle,
            more = more
        )

        /**
         * Represents a list of [Category].
         *
         * @property title the title of the list.
         * @property list the list of categories.
         * @property subtitle the subtitle of the list.
         * @property type the type of the list.
         * @property more the more data of the list.
         */
        data class Categories(
            override val title: String,
            override val list: List<Category>,
            override val subtitle: String? = null,
            override val type: Type = Type.Linear,
            override val more: PagedData<Category>? = null,
        ) : Lists<Category>(
            title = title,
            list = list,
            type = type,
            subtitle = subtitle,
            more = more
        )
    }

    /**
     * Represents a media item.
     *
     * @property media the media item.
     * @property loadTracks whether to load the tracks of the media item.
     */
    data class Item(
        val media: EchoMediaItem,
        //TODO: rename this to also be useful for showing Track with full width (without loading)
        val loadTracks: Boolean = false
    ) : Shelf()

    /**
     * Represents a category of media items.
     *
     * If [items] is not null, a "More" button will be displayed
     * and clicking on it will load a separate page for loading the [PagedData].
     *
     * If [items] is null, the category will act as a header.
     *
     * @property title the title of the category.
     * @property items the items of the category.
     * @property subtitle the subtitle of the category.
     * @property extras additional information about the category.
     */
    data class Category(
        val title: String,
        val items: PagedData<Shelf>?,
        val subtitle: String? = null,
        val extras: Map<String, String> = mapOf()
    ) : Shelf()

    fun sameAs(other: Shelf) = when (this) {
        is Item -> other is Item && media.sameAs(other.media)
        is Category -> other is Category && this.id == other.id
        is Categories -> other is Categories && this.id == other.id
        is Items -> other is Items && this.id == other.id
        is Tracks -> other is Tracks && this.id == other.id
    }

    val id
        get() = when (this) {
            is Item -> media.id
            else -> this.hashCode().toString()
        }
}