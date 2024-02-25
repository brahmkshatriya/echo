package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track

interface RadioClient {
    suspend fun radio(track: Track): Playlist.Full
    suspend fun radio(album: Album.Small): Playlist.Full
    suspend fun radio(artist: Artist.Small): Playlist.Full
    suspend fun radio(playlist: Playlist.Small): Playlist.Full
}