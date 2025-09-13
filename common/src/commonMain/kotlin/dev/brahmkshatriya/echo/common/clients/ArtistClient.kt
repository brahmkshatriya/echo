package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf

/**
 * Used to load an [Artist] and get its shelves.
 *
 * @see FollowClient
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
     * Gets the shelves of an artist.
     *
     * @param artist the artist to get the shelves of.
     * @return the feed containing the shelves.
     *
     * @see Feed
     */
    suspend fun loadFeed(artist: Artist) : Feed<Shelf>
}