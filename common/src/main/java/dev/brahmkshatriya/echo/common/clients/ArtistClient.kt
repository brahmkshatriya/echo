package dev.brahmkshatriya.echo.common.clients

import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import kotlinx.coroutines.flow.Flow

interface ArtistClient {
    suspend fun loadArtist(small: Artist): Artist
    suspend fun getMediaItems(artist: Artist): Flow<PagingData<MediaItemsContainer>>
}