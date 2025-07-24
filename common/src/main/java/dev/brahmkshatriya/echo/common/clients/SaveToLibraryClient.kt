package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.EchoMediaItem

/**
 * Used to save media items with [EchoMediaItem.isSavable] set to true to the library
 * @see EchoMediaItem.isSavable
 * @see LibraryFeedClient
 * @see MusicExtension
 */
interface SaveToLibraryClient {

    /**
     * Saves or removes a media item from the library.
     *
     * @param mediaItem the media item to save or remove.
     * @param save whether the media item should be saved or removed.
     */
   suspend fun saveToLibrary(mediaItem: EchoMediaItem, save:Boolean)

    /**
     * Checks if a media item is saved to the library.
     *
     * @param mediaItem the media item to check.
     * @return whether the media item is saved to the library.
     */
   suspend fun isSavedToLibrary(mediaItem: EchoMediaItem) : Boolean
}