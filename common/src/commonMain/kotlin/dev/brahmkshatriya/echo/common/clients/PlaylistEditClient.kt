package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track

/**
 * Used to allow editing of an editable playlist.
 *
 * - To allow editing of cover art, use [PlaylistEditCoverClient].
 * - To allow editing the privacy of a playlist, use [PlaylistEditPrivacyClient].
 * - To listen to changes in the playlist editor, use [PlaylistEditorListenerClient].
 *
 * @see Playlist.isEditable
 */
interface PlaylistEditClient : PlaylistClient {

    /**
     * Lists all the editable playlists.
     *
     * @param track the track to show the editable playlists for.
     * @return a list playlists that are editable with a boolean indicating if the track is in the playlist.
     *
     * @see Playlist
     */
    suspend fun listEditablePlaylists(track: Track?): List<Pair<Playlist, Boolean>>

    /**
     * Creates a new playlist.
     *
     * @param title the title of the playlist.
     * @param description the description of the playlist.
     * @return the created playlist.
     *
     * @see Playlist
     */
    suspend fun createPlaylist(title: String, description: String?): Playlist

    /**
     * Deletes a playlist.
     *
     * @param playlist the playlist to delete.
     *
     * @see Playlist
     */
    suspend fun deletePlaylist(playlist: Playlist)

    /**
     * Edits the metadata of a playlist.
     *
     * @param playlist the playlist to edit.
     * @param title the new title of the playlist.
     * @param description the new description of the playlist.
     */
    suspend fun editPlaylistMetadata(playlist: Playlist, title: String, description: String?)

    /**
     * Adds tracks to a playlist.
     *
     * @param playlist the playlist to add the tracks to.
     * @param tracks the tracks in the playlist.
     * @param index the index to add the tracks at.
     * @param new the new tracks to add.
     */
    suspend fun addTracksToPlaylist(
        playlist: Playlist, tracks: List<Track>, index: Int, new: List<Track>
    )

    /**
     * Removes tracks from a playlist.
     *
     * @param playlist the playlist to remove the tracks from.
     * @param tracks the tracks in the playlist.
     * @param indexes the indexes of the tracks to remove.
     */
    suspend fun removeTracksFromPlaylist(
        playlist: Playlist, tracks: List<Track>, indexes: List<Int>
    )

    /**
     * Moves a track in a playlist.
     *
     * @param playlist the playlist to move the track in.
     * @param tracks the tracks in the playlist.
     * @param fromIndex the index to move the tracks from.
     * @param toIndex the index to move the tracks to.
     */
    suspend fun moveTrackInPlaylist(
        playlist: Playlist, tracks: List<Track>, fromIndex: Int, toIndex: Int
    )
}