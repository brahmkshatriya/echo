package dev.brahmkshatriya.echo.common.data.clients

import dev.brahmkshatriya.echo.common.data.models.Playlist

interface PlaylistClient {
    suspend fun loadPlaylist(playlist: Playlist.Small): Playlist.Full
}