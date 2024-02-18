package dev.brahmkshatriya.echo.common.data.clients

import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.data.models.Album
import dev.brahmkshatriya.echo.common.data.models.MediaItemsContainer
import kotlinx.coroutines.flow.Flow

interface AlbumClient {
    suspend fun loadAlbum(small: Album.Small): Album.Full
    suspend fun getMediaItems(album: Album.Small): Flow<PagingData<MediaItemsContainer>>

}