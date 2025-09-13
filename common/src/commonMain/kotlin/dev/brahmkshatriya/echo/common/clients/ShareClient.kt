package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.EchoMediaItem

/**
 * Used for getting a link to share media items with [EchoMediaItem.isShareable] set to true.
 */
interface ShareClient {
    /**
     * When the user wants to share the given media item
     *
     * @param item The media item to share
     * @return url of the shared item
     */
    suspend fun onShare(item: EchoMediaItem): String
}