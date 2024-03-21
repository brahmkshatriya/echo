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
}