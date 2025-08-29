package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.QuickSearchItem

/**
 * Used to show search feed with a custom quick search implementation.
 *
 * @see QuickSearchItem
 * @see Feed
 * @see MusicExtension
 */
interface QuickSearchClient : SearchFeedClient {
    /**
     * Used for quick searching (suggestions below the search bar).
     * This is a lightweight search that returns a list of items
     *
     * @param query the query to search for, will be empty if the user hasn't typed anything.
     * @return the quick search items.
     *
     * @see QuickSearchItem
     */
    suspend fun quickSearch(query: String): List<QuickSearchItem>

    /**
     * Deletes a quick search item.
     *
     * @param item the item to delete.
     */
    suspend fun deleteQuickSearch(item: QuickSearchItem)
}