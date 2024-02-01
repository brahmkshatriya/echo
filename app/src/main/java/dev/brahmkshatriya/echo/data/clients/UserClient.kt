package dev.brahmkshatriya.echo.data.clients

import androidx.paging.PagingData
import dev.brahmkshatriya.echo.data.models.Playlist
import dev.brahmkshatriya.echo.data.models.Track
import dev.brahmkshatriya.echo.data.models.User
import kotlinx.coroutines.flow.Flow

interface UserClient {
    suspend fun login(data:String)
    suspend fun logout()
    suspend fun user() : User.WithCover?
    suspend fun likedTracks() : Flow<PagingData<Track.Small>>
    suspend fun playlists() : Flow<PagingData<Playlist.WithCover>>
    suspend fun createPlaylist(name:String, description:String?, tracks: List<Track.Small>) : Playlist.WithCover
    suspend fun like(track: Track.Small): Boolean
    suspend fun unlike(track: Track.Small): Boolean
}