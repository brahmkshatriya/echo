package dev.brahmkshatriya.echo.common.models

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

sealed class EchoMediaItem {
    data class TrackItem(val track: Track) : EchoMediaItem()
    data class AlbumItem(val album: Album.WithCover) : EchoMediaItem()
    data class ArtistItem(val artist: Artist.WithCover) : EchoMediaItem()
    data class PlaylistItem(val playlist: Playlist.WithCover) : EchoMediaItem()

    companion object {
        fun Track.toMediaItem() = TrackItem(this)
        fun Album.WithCover.toMediaItem() = AlbumItem(this)
        fun Artist.WithCover.toMediaItem() = ArtistItem(this)
        fun Playlist.WithCover.toMediaItem() = PlaylistItem(this)

        fun EchoMediaItem.toMediaItemsContainer() = when (this) {
            is TrackItem -> MediaItemsContainer.TrackItem(this.track)
            is AlbumItem -> MediaItemsContainer.AlbumItem(this.album)
            is ArtistItem -> MediaItemsContainer.ArtistItem(this.artist)
            is PlaylistItem -> MediaItemsContainer.PlaylistItem(this.playlist)
        }

        fun List<EchoMediaItem>.toMediaItemsContainer(
            title: String, subtitle: String? = null, flow: Flow<PagingData<EchoMediaItem>>? = null
        ) = MediaItemsContainer.Category(title, this, subtitle, flow)
    }

    override fun equals(other: Any?): Boolean {
        if (other is EchoMediaItem) {
            return when (this) {
                is TrackItem -> this.track.uri == (other as? TrackItem)?.track?.uri
                is AlbumItem -> this.album.uri == (other as? AlbumItem)?.album?.uri
                is ArtistItem -> this.artist.uri == (other as? ArtistItem)?.artist?.uri
                is PlaylistItem -> this.playlist.uri == (other as? PlaylistItem)?.playlist?.uri
            }
        }
        return false
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}