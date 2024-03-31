package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User

interface UserClient {
    suspend fun login(data: String)
    suspend fun logout()
    suspend fun user(): User?
    suspend fun loadUser(user: User): User
    suspend fun createPlaylist(name: String, description: String?, tracks: List<Track>)
    suspend fun like(track: Track): Boolean
    suspend fun unlike(track: Track): Boolean
    suspend fun subscribe(artist: Artist): Boolean
    suspend fun unsubscribe(artist: Artist): Boolean
    fun getMediaItems(it: User): PagedData<MediaItemsContainer>
}