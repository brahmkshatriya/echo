package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.helpers.PagedData

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
            title: String, subtitle: String? = null, more: PagedData<EchoMediaItem>? = null
        ) = MediaItemsContainer.Category(title, this, subtitle, more)

    }

    fun toMediaItemsContainer() = when (this) {
        is TrackItem -> MediaItemsContainer.TrackItem(this.track)
        is AlbumItem -> MediaItemsContainer.AlbumItem(this.album)
        is ArtistItem -> MediaItemsContainer.ArtistItem(this.artist)
        is PlaylistItem -> MediaItemsContainer.PlaylistItem(this.playlist)
    }

}