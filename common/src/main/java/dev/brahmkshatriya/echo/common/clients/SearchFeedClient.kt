package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab

/**
 * Used to show the search the feed.
 *
 * @see QuickSearchItem
 * @see Tab
 * @see Shelf
 * @see PagedData
 */
interface SearchFeedClient {
    /**
     * Searches for quick search items.
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

    /**
     * Get the tabs for the search feed.
     *
     * @param query the query to search for, will be empty if the user hasn't typed anything.
     * @return the tabs.
     *
     * @see Tab
     */
    suspend fun searchTabs(query: String): List<Tab>

    /**
     * Gets the search feed.
     *
     * @param query the query to search for, will be empty if the user hasn't typed anything.
     * @param tab the tab to search in.
     * @return the feed.
     *
     * @see Tab
     * @see Feed
     */
    fun searchFeed(query: String, tab: Tab?): Feed
}