package dev.brahmkshatriya.echo.data.clients

import androidx.paging.PagingData
import dev.brahmkshatriya.echo.data.models.Artist
import dev.brahmkshatriya.echo.data.models.MediaItemsContainer
import kotlinx.coroutines.flow.Flow

interface ArtistClient {
    suspend fun loadArtist(small: Artist.Small): Artist.Full
    suspend fun getMediaItems(artist: Artist.Full): Flow<PagingData<MediaItemsContainer>>
}