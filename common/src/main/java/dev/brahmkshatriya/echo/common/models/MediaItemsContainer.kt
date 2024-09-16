package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.helpers.PagedData

sealed class MediaItemsContainer {

    data class Category(
        val title: String,
        val list: List<EchoMediaItem>,
        val subtitle: String? = null,
        val more: PagedData<EchoMediaItem>? = null
    ) : MediaItemsContainer()

    data class Tracks(
        val title: String,
        val list: List<Track>,
        val type: Type,
        val subtitle: String? = null,
        val more: PagedData<Track>? = null
    ) : MediaItemsContainer() {
        //TODO
        enum class Type {
            List, Grid
        }
    }

    data class Item(
        val media: EchoMediaItem
    ) : MediaItemsContainer()

    data class Container(
        val title: String,
        val subtitle: String? = null,
        val more: PagedData<MediaItemsContainer>? = null,
        val extra: Map<String, String> = mapOf()
    ) : MediaItemsContainer()

    fun sameAs(other: MediaItemsContainer) = when (this) {
        is Category -> other is Category && this.id == other.id
        is Item -> other is Item && media.sameAs(other.media)
        is Container -> other is Container && this.id == other.id
        is Tracks -> other is Tracks && this.id == other.id
    }

    val id
        get() = when (this) {
            is Item -> media.id
            else -> this.hashCode().toString()
        }
}