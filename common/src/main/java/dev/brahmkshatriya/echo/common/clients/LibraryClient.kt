package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track

interface LibraryClient {
    suspend fun getLibraryGenres(): List<Genre>
    fun getLibraryFeed(genre: Genre?): PagedData<MediaItemsContainer>
    suspend fun listEditablePlaylists(): List<Playlist>
    suspend fun likeTrack(track: Track, liked: Boolean): Boolean
    suspend fun createPlaylist(title: String, description: String?): Playlist
    suspend fun deletePlaylist(playlist: Playlist)
    suspend fun editPlaylistMetadata(playlist: Playlist, title: String, description: String?)
    suspend fun addTracksToPlaylist(playlist: Playlist, index: Int?, tracks: List<Track>)
    suspend fun removeTracksFromPlaylist(playlist: Playlist, trackIndexes: List<Track>)
    suspend fun moveTrackInPlaylist(playlist: Playlist, fromIndex: Int, toIndex: Int)
}