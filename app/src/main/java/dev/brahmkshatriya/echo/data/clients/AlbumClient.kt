package dev.brahmkshatriya.echo.data.clients

import androidx.paging.PagingData
import dev.brahmkshatriya.echo.data.models.Album
import dev.brahmkshatriya.echo.data.models.MediaItem
import dev.brahmkshatriya.echo.data.models.Playlist
import kotlinx.coroutines.flow.Flow

interface AlbumClient {
    suspend fun loadAlbum(small: Album.Small): Album.Full
    suspend fun getMediaItems(album: Album.Small): Map<String, Flow<PagingData<MediaItem>>>
    suspend fun radio(album: Album.Small): Playlist.Full
}