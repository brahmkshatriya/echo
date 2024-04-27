package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Playlist

interface EditPlayerListenerClient {
    suspend fun onEnterPlaylistEditor(playlist: Playlist)
    suspend fun onExitPlaylistEditor(playlist: Playlist)
}