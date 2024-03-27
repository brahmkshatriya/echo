package dev.brahmkshatriya.echo.common.models

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

sealed class MediaItemsContainer {

    data class Category(
        val title: String,
        val list: List<EchoMediaItem>,
        val subtitle: String? = null,
        val flow: Flow<PagingData<EchoMediaItem>>? = null
    ) : MediaItemsContainer()

    data class TrackItem(val track: Track) : MediaItemsContainer()
    data class AlbumItem(val album: Album) : MediaItemsContainer()
    data class ArtistItem(val artist: Artist) : MediaItemsContainer()
    data class PlaylistItem(val playlist: Playlist) : MediaItemsContainer()

    fun sameAs(newItem: MediaItemsContainer) = when (this) {
        is Category -> newItem is Category && this == newItem
        is TrackItem -> newItem is TrackItem && this.track.id == newItem.track.id
        is AlbumItem -> newItem is AlbumItem && this.album.id == newItem.album.id
        is ArtistItem -> newItem is ArtistItem && this.artist.id == newItem.artist.id
        is PlaylistItem -> newItem is PlaylistItem && this.playlist.id == newItem.playlist.id
    }
}