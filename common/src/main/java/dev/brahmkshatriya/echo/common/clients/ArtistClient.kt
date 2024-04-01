package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer

interface ArtistClient {
    suspend fun loadArtist(small: Artist): Artist
    fun getMediaItems(artist: Artist): PagedData<MediaItemsContainer>
}