package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track

/**
 * Used to load an [Album], load its tracks and get its shelves.
 *
 * @see MusicExtension
 */
interface AlbumClient {

    /**
     * Loads an album, the unloaded album may only have the id and title.
     *
     * @param album the album to load.
     * @return the loaded album.
     */
    suspend fun loadAlbum(album: Album): Album

    /**
     * Loads the tracks of an album.
     *
     * @param album the loaded album to load the tracks of.
     * @return the paged tracks or null if the tracks cannot be loaded for this [album].
     *
     * @see Feed
     * @see Track
     */
    suspend fun loadTracks(album: Album): Feed<Track>?

    /**
     * Gets the feed of an album.
     *
     * @param album the album to get the feed of.
     * @return the feed of the album, or null if not available.
     *
     * @see Feed
     * @see Album
     */
    suspend fun loadFeed(album: Album): Feed<Shelf>?
}