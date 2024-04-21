package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User

interface ShareClient {
    suspend fun onShare(artist: Artist): String
    suspend fun onShare(track: Track): String
    suspend fun onShare(album: Album): String
    suspend fun onShare(playlist: Playlist): String
    suspend fun onShare(user: User): String
}