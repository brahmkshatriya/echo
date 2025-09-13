package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track

/**
 * Used to listen to playlist editor events.
 *
 * @see PlaylistEditClient
 */
interface PlaylistEditorListenerClient : PlaylistEditClient {

    /**
     * Called when entering the playlist editor.
     *
     * @param playlist the playlist being edited.
     * @param tracks the tracks in the playlist.
     */
    suspend fun onEnterPlaylistEditor(playlist: Playlist, tracks: List<Track>)

    /**
     * Called when exiting the playlist editor.
     *
     * @param playlist the playlist being edited.
     * @param tracks the tracks in the playlist.
     */
    suspend fun onExitPlaylistEditor(playlist: Playlist, tracks: List<Track>)
}