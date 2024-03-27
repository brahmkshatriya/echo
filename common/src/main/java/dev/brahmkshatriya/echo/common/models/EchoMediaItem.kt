package dev.brahmkshatriya.echo.common.models

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

sealed class EchoMediaItem {
    data class TrackItem(val track: Track) : EchoMediaItem()
    data class AlbumItem(val album: Album) : EchoMediaItem()
    data class ArtistItem(val artist: Artist) : EchoMediaItem()
    data class PlaylistItem(val playlist: Playlist) : EchoMediaItem()

    companion object {
        fun Track.toMediaItem() = TrackItem(this)
        fun Album.toMediaItem() = AlbumItem(this)
        fun Artist.toMediaItem() = ArtistItem(this)
        fun Playlist.toMediaItem() = PlaylistItem(this)

        fun List<EchoMediaItem>.toMediaItemsContainer(
            title: String, subtitle: String? = null, flow: Flow<PagingData<EchoMediaItem>>? = null
        ) = MediaItemsContainer.Category(title, this, subtitle, flow)

    }

    fun toMediaItemsContainer() = when (this) {
        is TrackItem -> MediaItemsContainer.TrackItem(this.track)
        is AlbumItem -> MediaItemsContainer.AlbumItem(this.album)
        is ArtistItem -> MediaItemsContainer.ArtistItem(this.artist)
        is PlaylistItem -> MediaItemsContainer.PlaylistItem(this.playlist)
    }

    fun sameAs(other: EchoMediaItem) = when (this) {
        is TrackItem -> other is TrackItem && this.track.id == other.track.id
        is AlbumItem -> other is AlbumItem && this.album.id == other.album.id
        is ArtistItem -> other is ArtistItem && this.artist.id == other.artist.id
        is PlaylistItem -> other is PlaylistItem && this.playlist.id == other.playlist.id
    }
}