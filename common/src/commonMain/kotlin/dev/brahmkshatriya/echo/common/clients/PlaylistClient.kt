package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track

/**
 * Used to show the playlist and get its tracks.
 *
 * @see Playlist
 * @see Track
 * @see Feed
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
    suspend fun loadTracks(playlist: Playlist): Feed<Track>

    /**
     * Gets the feed of a playlist.
     *
     * @param playlist the playlist to get the feed of.
     * @return the feed of the playlist, or null if not available.
     *
     * @see Feed
     */
    suspend fun loadFeed(playlist: Playlist): Feed<Shelf>?
}