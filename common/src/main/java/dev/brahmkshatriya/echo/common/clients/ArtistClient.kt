package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Shelf

interface ArtistClient {
    suspend fun loadArtist(small: Artist): Artist
    fun getShelves(artist: Artist): PagedData<Shelf>
}