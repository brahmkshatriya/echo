package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track

interface PlaylistClient {
    suspend fun loadPlaylist(playlist: Playlist): Playlist
    fun loadTracks(playlist: Playlist): PagedData<Track>
    fun getShelves(playlist: Playlist): PagedData<Shelf>
}