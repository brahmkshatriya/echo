package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import dev.brahmkshatriya.echo.extension.spotify.models.Item as SpotifyItem

@Serializable
data class LibraryV3 (
    val data: Data? = null
) {

    @Serializable
    data class Data(
        val me: Me? = null
    )

    @Serializable
    data class Me(
        val libraryV3: LibraryV3Class? = null
    )

    @Serializable
    data class LibraryV3Class(
        @SerialName("__typename")
        val typename: String? = null,

        val availableFilters: List<Filter>? = null,
        val availableSortOrders: List<Filter>? = null,
        val breadcrumbs: JsonArray? = null,
        val items: List<Item>,
        val pagingInfo: PagingInfo? = null,
        val selectedFilters: JsonArray? = null,
        val selectedSortOrder: Filter? = null,
        val totalCount: Long? = null
    )

    @Serializable
    data class Filter(
        val id: String? = null,
        val name: String? = null
    )

    @Serializable
    data class Item(
        val addedAt: Date? = null,
        val depth: Long? = null,
        val item: SpotifyItem.Wrapper? = null,
        val pinnable: Boolean? = null,
        val pinned: Boolean? = null,
        val playedAt: Date? = null
    )

}