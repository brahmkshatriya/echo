package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.EchoMediaItem

/**
 * Used to save media items with [EchoMediaItem.isSaveable] set to true to the library
 * @see EchoMediaItem.isSaveable
 * @see LibraryFeedClient
 * @see MusicExtension
 */
interface SaveClient {

    /**
     * Saves or removes a media item from the library.
     *
     * @param item the media item to save or remove.
     * @param shouldSave whether the media item should be saved or removed.
     */
   suspend fun saveToLibrary(item: EchoMediaItem, shouldSave:Boolean)

    /**
     * Checks if a media item is saved to the library.
     *
     * @param item the media item to check.
     * @return whether the media item is saved to the library.
     */
   suspend fun isItemSaved(item: EchoMediaItem) : Boolean
}