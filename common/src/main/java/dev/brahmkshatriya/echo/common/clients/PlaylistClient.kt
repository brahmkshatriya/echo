package dev.brahmkshatriya.echo.common.clients

import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistClient {
    suspend fun loadPlaylist(playlist: Playlist): Playlist
    suspend fun getMediaItems(playlist: Playlist): Flow<PagingData<MediaItemsContainer>>
}