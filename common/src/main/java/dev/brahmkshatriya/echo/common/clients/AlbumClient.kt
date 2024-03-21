package dev.brahmkshatriya.echo.common.clients

import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import kotlinx.coroutines.flow.Flow

interface AlbumClient {
    suspend fun loadAlbum(small: Album): Album
    suspend fun getMediaItems(album: Album): Flow<PagingData<MediaItemsContainer>>
}