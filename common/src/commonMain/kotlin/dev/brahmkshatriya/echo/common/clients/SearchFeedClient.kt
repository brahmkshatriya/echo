package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf

/**
 * Used to show the search the feed.
 *
 * @see Feed
 * @see MusicExtension
 */
interface SearchFeedClient {

    /**
     * Gets the search feed.
     *
     * @param query the query to search for, will be empty if the user hasn't typed anything.
     * @return the feed.
     *
     * @see Feed
     */
    suspend fun loadSearchFeed(query: String): Feed<Shelf>
}