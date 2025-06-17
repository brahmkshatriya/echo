package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab

/**
 * Used to show the library feed and get its tabs.
 *
 * @see Tab
 * @see Shelf
 * @see PagedData
 */
interface LibraryFeedClient {
    /**
     * Gets the library tabs.
     *
     * @return the library tabs.
     *
     * @see Tab
     */
    suspend fun getLibraryTabs(): List<Tab>

    /**
     * Gets the library feed.
     * [tab] will be null if [getLibraryTabs] returned an empty list.
     *
     * @param tab the tab to get the feed of.
     * @return the feed.
     *
     * @see Tab
     * @see Feed
     */
    fun getLibraryFeed(tab: Tab?): Feed
}