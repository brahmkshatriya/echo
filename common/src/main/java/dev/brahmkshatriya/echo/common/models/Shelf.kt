package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.helpers.PagedData

sealed class Shelf {

    sealed class Lists<T : Any>(
        val title: String,
        val list: List<T>,
        val subtitle: String?,
        val type: Type,
        val more: PagedData<T>?
    ) : Shelf() {


        enum class Type {
            Linear, Grid
        }

        class Items(
            title: String,
            list: List<EchoMediaItem>,
            subtitle: String? = null,
            type: Type = Type.Linear,
            more: PagedData<EchoMediaItem>? = null
        ) : Lists<EchoMediaItem>(
            title = title,
            list = list,
            subtitle = subtitle,
            type = type,
            more = more
        )

        class Tracks(
            title: String,
            list: List<Track>,
            subtitle: String? = null,
            type: Type = Type.Linear,
            val isNumbered: Boolean = false,
            more: PagedData<Track>? = null
        ) : Lists<Track>(
            title = title,
            list = list,
            type = type,
            subtitle = subtitle,
            more = more
        )

        class Categories(
            title: String,
            list: List<Category>,
            subtitle: String? = null,
            type: Type = Type.Linear,
            more: PagedData<Category>? = null,
        ) : Lists<Category>(
            title = title,
            list = list,
            type = type,
            subtitle = subtitle,
            more = more
        )
    }


    data class Item(
        val media: EchoMediaItem
    ) : Shelf()

    data class Category(
        val title: String,
        val items: PagedData<Shelf>?,
        val subtitle: String? = null,
        val extras: Map<String, String> = mapOf()
    ) : Shelf()

    fun sameAs(other: Shelf) = when (this) {
        is Item -> other is Item && media.sameAs(other.media)
        is Category -> other is Category && this.id == other.id
        is Lists.Categories -> other is Lists.Categories && this.id == other.id
        is Lists.Items -> other is Lists.Items && this.id == other.id
        is Lists.Tracks -> other is Lists.Tracks && this.id == other.id
    }

    val id
        get() = when (this) {
            is Item -> media.id
            else -> this.hashCode().toString()
        }
}