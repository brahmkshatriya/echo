package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf

/**
 * Used to show the library feed and get its tabs.
 *
 * @see Feed
 * @see MusicExtension
 */
interface LibraryFeedClient {

    /**
     * Gets the library feed.
     * Checkout [Feed] for more information.
     */
    suspend fun loadLibraryFeed(): Feed<Shelf>

}