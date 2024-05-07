package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track

interface LibraryClient : PlaylistClient, AlbumClient {
    suspend fun getLibraryTabs(): List<Tab>
    fun getLibraryFeed(tab: Tab?): PagedData<MediaItemsContainer>
    suspend fun listEditablePlaylists(): List<Playlist>
    suspend fun likeTrack(track: Track, liked: Boolean): Boolean
    suspend fun createPlaylist(title: String, description: String?): Playlist
    suspend fun deletePlaylist(playlist: Playlist)
    suspend fun editPlaylistMetadata(playlist: Playlist, title: String, description: String?)
    suspend fun addTracksToPlaylist(
        playlist: Playlist, tracks: List<Track>, index: Int, new: List<Track>
    )

    suspend fun removeTracksFromPlaylist(
        playlist: Playlist, tracks: List<Track>, indexes: List<Int>
    )

    suspend fun moveTrackInPlaylist(
        playlist: Playlist, tracks: List<Track>, fromIndex: Int, toIndex: Int
    )
}