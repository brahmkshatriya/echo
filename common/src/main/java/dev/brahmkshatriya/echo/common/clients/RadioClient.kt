package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User

/**
 * Used to load the radio for track that is currently playing
 * and to show radio buttons for albums, artists, users and playlists.
 *
 * @see Radio
 * @see TrackClient
 */
interface RadioClient : TrackClient {
    /**
     * Loads the tracks for a radio.
     *
     * @param radio the radio to load the tracks of.
     * @return the paged tracks.
     *
     * @see PagedData
     * @see Track
     */
    fun loadTracks(radio: Radio): PagedData<Track>

    /**
     * Creates a radio for a track.
     *
     * Make sure the radio tracks does not include this [track].
     *
     * @param track the track to create the radio for.
     * @param context the context of the track.
     * @return the created radio.
     *
     * @see Track
     * @see EchoMediaItem
     * @see Radio
     */
    suspend fun radio(track: Track, context: EchoMediaItem?): Radio

    /**
     * Creates a radio for an album.
     *
     * @param album the album to create the radio for.
     * @return the created radio.
     *
     * @see Album
     * @see Radio
     */
    suspend fun radio(album: Album): Radio

    /**
     * Creates a radio for an artist.
     *
     * @param artist the artist to create the radio for.
     * @return the created radio.
     *
     * @see Artist
     * @see Radio
     */
    suspend fun radio(artist: Artist): Radio

    /**
     * Creates a radio for a user.
     *
     * @param user the user to create the radio for.
     * @return the created radio.
     *
     * @see User
     * @see Radio
     */
    suspend fun radio(user: User): Radio

    /**
     * Creates a radio for a playlist.
     *
     * @param playlist the playlist to create the radio for.
     * @return the created radio.
     *
     * @see Playlist
     * @see Radio
     */
    suspend fun radio(playlist: Playlist): Radio
}