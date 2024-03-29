package dev.brahmkshatriya.echo.common.clients

import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import kotlinx.coroutines.flow.Flow

interface UserClient {
    suspend fun login(data:String)
    suspend fun logout()
    suspend fun user() : User?
    suspend fun likedTracks() : Flow<PagingData<Track>>
    suspend fun playlists() : Flow<PagingData<Playlist>>
    suspend fun createPlaylist(name:String, description:String?, tracks: List<Track>)
    suspend fun like(track: Track): Boolean
    suspend fun unlike(track: Track): Boolean
    suspend fun subscribe(artist: Artist): Boolean
    suspend fun unsubscribe(artist: Artist): Boolean
}