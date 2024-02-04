package dev.brahmkshatriya.echo.data.clients

import androidx.paging.PagingData
import dev.brahmkshatriya.echo.data.models.Artist
import dev.brahmkshatriya.echo.data.models.Playlist
import dev.brahmkshatriya.echo.data.models.Track
import dev.brahmkshatriya.echo.data.models.User
import kotlinx.coroutines.flow.Flow

interface UserClient {
    suspend fun login(data:String)
    suspend fun logout()
    suspend fun user() : User.WithCover?
    suspend fun likedTracks() : Flow<PagingData<Track>>
    suspend fun playlists() : Flow<PagingData<Playlist.WithCover>>
    suspend fun createPlaylist(name:String, description:String?, tracks: List<Track>) : Playlist.WithCover
    suspend fun like(track: Track): Boolean
    suspend fun unlike(track: Track): Boolean
    suspend fun subscribe(artist: Artist.Small): Boolean
    suspend fun unsubscribe(artist: Artist.Small): Boolean
}