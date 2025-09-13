package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Playlist
import java.io.File

/**
 * Used to allow editing the cover of a playlist.
 *
 * @see PlaylistEditClient
 */
interface PlaylistEditCoverClient : PlaylistEditClient {

    /**
     * Edits the cover of a playlist.
     *
     * @param playlist the playlist to edit the cover of.
     * @param cover the new cover of the playlist.
     */
    suspend fun editPlaylistCover(playlist: Playlist, cover: File?)
}