package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User

interface RadioClient : PlaylistClient {
    suspend fun radio(track: Track): Playlist
    suspend fun radio(album: Album): Playlist
    suspend fun radio(artist: Artist): Playlist
    suspend fun radio(user: User) : Playlist
    suspend fun radio(playlist: Playlist): Playlist
}