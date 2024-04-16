package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track

interface LibraryClient {
    suspend fun getLibraryGenres(): List<Genre>
    fun getLibraryFeed(genre: Genre?): PagedData<MediaItemsContainer>
    suspend fun likeTrack(track: Track, liked: Boolean)
    suspend fun createPlaylist(name: String, description: String?): Playlist
    suspend fun deletePlaylist(playlist: Playlist)
    suspend fun addTracksToPlaylist(playlist: Playlist, tracks: List<Track>)
    suspend fun removeTrackFromPlaylist(playlist: Playlist, tracks: List<Track>)
}