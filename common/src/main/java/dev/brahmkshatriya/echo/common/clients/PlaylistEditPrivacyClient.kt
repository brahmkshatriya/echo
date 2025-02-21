package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Playlist

/**
 * Used to edit the privacy of a playlist.
 *
 * @see Playlist.isPrivate
 * @see PlaylistEditClient
 */
interface PlaylistEditPrivacyClient : PlaylistEditClient {
    /**
     * Sets the privacy of a playlist.
     *
     * @param playlist the playlist to set the privacy of.
     * @param isPrivate whether the playlist should be private.
     */
    //TODO
    suspend fun setPrivacy(playlist: Playlist, isPrivate: Boolean)
}