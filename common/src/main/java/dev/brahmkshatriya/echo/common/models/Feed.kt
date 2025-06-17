package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.helpers.PagedData

/**
 * Represents a feed of shelves, which can be used in various contexts like home, library, or search.
 *
 * @property pagedData The paged data containing shelves.
 * @property showPlayButton Whether to show the play button.
 * @property showShuffleButton Whether to show the shuffle button.
 * @property showSearchButton Whether to show the search button.
 * @property showFilterButton Whether to show the filter button.
 *
 * @see PagedData
 * @see Shelf
 */
data class Feed(
    val pagedData: PagedData<Shelf>,
    val showPlayButton: Boolean = false,
    val showShuffleButton: Boolean = false,
    val showSearchButton: Boolean = false,
    val showFilterButton: Boolean = false,
) {
    companion object {
        fun PagedData<Shelf>.toFeed(
            showPlayButton: Boolean = false,
            showShuffleButton: Boolean = false,
            showSearchButton: Boolean = false,
            showFilterButton: Boolean = false
        ) = Feed(
            pagedData = this,
            showPlayButton = showPlayButton,
            showShuffleButton = showShuffleButton,
            showSearchButton = showSearchButton,
            showFilterButton = showFilterButton
        )
    }
}