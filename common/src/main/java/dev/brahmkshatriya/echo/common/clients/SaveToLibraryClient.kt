package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.EchoMediaItem

interface SaveToLibraryClient {
   suspend fun saveToLibrary(mediaItem: EchoMediaItem)
   suspend fun removeFromLibrary(mediaItem: EchoMediaItem)
   suspend fun isSavedToLibrary(mediaItem: EchoMediaItem)
}