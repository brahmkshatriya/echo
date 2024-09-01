package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Track

interface AlbumClient {
    suspend fun loadAlbum(album: Album): Album
    fun loadTracks(album: Album): PagedData<Track>
    fun getMediaItems(album: Album): PagedData<MediaItemsContainer>
}