package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track

interface AlbumClient {
    suspend fun loadAlbum(album: Album): Album
    fun loadTracks(album: Album): PagedData<Track>
    fun getShelves(album: Album): PagedData<Shelf>
}