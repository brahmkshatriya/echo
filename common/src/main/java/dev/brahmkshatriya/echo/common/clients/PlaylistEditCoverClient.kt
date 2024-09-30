package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Playlist
import java.io.File

interface PlaylistEditCoverClient : PlaylistEditClient {
    fun editPlaylistCover(playlist: Playlist, cover: File?)
}