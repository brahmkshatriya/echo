package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.helpers.PagedData

sealed class MediaItemsContainer {

    data class Category(
        val title: String,
        val list: List<EchoMediaItem>,
        val subtitle: String? = null,
        val more: PagedData<EchoMediaItem>? = null
    ) : MediaItemsContainer()

    data class Item(
        val media: EchoMediaItem
    ) : MediaItemsContainer()

    fun sameAs(other: MediaItemsContainer) = when (this) {
        is Category -> other is Category && this == other
        is Item -> other is Item && media.sameAs(other.media)
    }

    val id get() = when (this) {
        is Category -> this.hashCode().toString()
        is Item -> media.id
    }
}