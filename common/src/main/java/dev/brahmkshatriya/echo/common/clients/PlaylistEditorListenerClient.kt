package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track

interface PlaylistEditorListenerClient : PlaylistEditClient {
    suspend fun onEnterPlaylistEditor(playlist: Playlist, tracks: List<Track>)
    suspend fun onExitPlaylistEditor(playlist: Playlist, tracks: List<Track>)
}