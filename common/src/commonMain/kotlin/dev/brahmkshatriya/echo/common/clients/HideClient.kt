package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.EchoMediaItem

/**
 * Used to hide an item from the user's feed.
 * with the [EchoMediaItem.isHideable] set to true.
 */
interface HideClient {

    /**
     * Hides or unhides a item from the user's feed.
     *
     * @param item the item to hide or unhide.
     * @param shouldHide whether the item should be hidden or unhidden.
     */
    suspend fun hideItem(item: EchoMediaItem, shouldHide: Boolean)

    /**
     * Checks if a item is hidden.
     *
     * @param item the item to check.
     * @return true if the track is hidden, false otherwise.
     */
    suspend fun isItemHidden(item: EchoMediaItem): Boolean
}