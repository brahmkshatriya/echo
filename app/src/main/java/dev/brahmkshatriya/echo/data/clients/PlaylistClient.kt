package dev.brahmkshatriya.echo.data.clients

import dev.brahmkshatriya.echo.data.models.Playlist

interface PlaylistClient {
    suspend fun loadPlaylist(playlist: Playlist.Small): Playlist.Full
}