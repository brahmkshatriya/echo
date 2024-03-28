package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist

interface PlaylistClient {
    suspend fun loadPlaylist(playlist: Playlist): Playlist
    fun getMediaItems(playlist: Playlist): PagedData<MediaItemsContainer>
}