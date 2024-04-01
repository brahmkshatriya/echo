package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer

interface AlbumClient {
    suspend fun loadAlbum(small: Album): Album
    fun getMediaItems(album: Album): PagedData<MediaItemsContainer>
}