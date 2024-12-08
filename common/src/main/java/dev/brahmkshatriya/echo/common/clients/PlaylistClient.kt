package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track

/**
 * Used to show the playlist and get its tracks.
 *
 * @see Playlist
 * @see Track
 * @see Shelf
 * @see PagedData
 */
interface PlaylistClient {

    /**
     * Loads a playlist.
     *
     * @param playlist the playlist to load.
     * @return the loaded playlist.
     *
     * @see Playlist
     */
    suspend fun loadPlaylist(playlist: Playlist): Playlist

    /**
     * Loads the tracks of a playlist.
     *
     * @param playlist the playlist to load the tracks of.
     * @return the paged tracks.
     *
     * @see PagedData
     * @see Track
     */
    fun loadTracks(playlist: Playlist): PagedData<Track>

    /**
     * Gets the shelves of a playlist.
     *
     * @param playlist the playlist to get the shelves of.
     * @return the paged shelves.
     *
     * @see PagedData
     * @see Shelf
     */
    fun getShelves(playlist: Playlist): PagedData<Shelf>
}