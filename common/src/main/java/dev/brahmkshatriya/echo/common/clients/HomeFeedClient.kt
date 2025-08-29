package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf

/**
 * Used to show the home feed and get its tabs.
 *
 * @see Feed
 * @see MusicExtension
 */
interface HomeFeedClient {

    /**
     * Gets the home feed.
     * Checkout [Feed] for more information.
     */
    suspend fun loadHomeFeed(): Feed<Shelf>
}
