package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab

/**
 * Used to show the home feed and get its tabs.
 *
 * @see Tab
 * @see Shelf
 * @see PagedData
 */
interface HomeFeedClient {

    /**
     * Gets the home tabs.
     *
     * @return the home tabs.
     *
     * @see Tab
     */
    suspend fun getHomeTabs(): List<Tab>

    /**
     * Gets the home feed.
     * [tab] will be null if [getHomeTabs] returned an empty list.
     *
     * @param tab the tab to get the feed of.
     * @return the feed.
     *
     * @see Tab
     * @see Feed
     */
    fun getHomeFeed(tab: Tab?): Feed

}
