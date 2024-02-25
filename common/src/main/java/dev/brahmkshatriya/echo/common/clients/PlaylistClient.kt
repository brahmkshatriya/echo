package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Playlist

interface PlaylistClient {
    suspend fun loadPlaylist(playlist: Playlist.Small): Playlist.Full
}