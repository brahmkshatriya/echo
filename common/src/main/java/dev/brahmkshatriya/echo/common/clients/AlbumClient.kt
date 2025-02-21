package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
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
     * @return the paged tracks.
     *
     * @see PagedData
     * @see Track
     */
    fun loadTracks(album: Album): PagedData<Track>

    /**
     * Gets the shelves of an album. (Like "More from this artist", "Similar albums", etc.)
     *
     * @param album the album to get the shelves of.
     * @return the paged shelves.
     *
     * @see PagedData
     * @see Shelf
     */
    fun getShelves(album: Album): PagedData<Shelf>
}