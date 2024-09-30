package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Playlist

//TODO
interface PlaylistEditPrivacyClient : PlaylistEditClient {
    suspend fun setPrivacy(playlist: Playlist, isPrivate: Boolean)
}