package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Shelf

/**
 * Used to load an [Artist] and get its shelves.
 *
 * @see ArtistFollowClient
 * @see MusicExtension
 */
interface ArtistClient {

    /**
     * Loads an artist, the unloaded artist may only have the id and title.
     *
     * @param artist the artist to load.
     * @return the loaded artist.
     */
    suspend fun loadArtist(artist: Artist): Artist

    /**
     * Gets the shelves of an artist. (Like "Top tracks", "Albums", etc.)
     *
     * @param artist the artist to get the shelves of.
     * @return the paged shelves.
     *
     * @see PagedData
     * @see Shelf
     */
    fun getShelves(artist: Artist): PagedData<Shelf>
}