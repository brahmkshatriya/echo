package dev.brahmkshatriya.echo.data.models

sealed class MediaItem {
    data class TrackItem(val track: Track) : MediaItem()
    data class AlbumItem(val album: Album.WithCover) : MediaItem()
    data class ArtistItem(val artist: Artist.WithCover) : MediaItem()
    data class PlaylistItem(val playlist: Playlist.WithCover) : MediaItem()

    companion object {
        fun Track.toMediaItem() = TrackItem(this).also { println(it.track.title) }
        fun Album.WithCover.toMediaItem() = AlbumItem(this)
        fun Artist.WithCover.toMediaItem() = ArtistItem(this)
        fun Playlist.WithCover.toMediaItem() = PlaylistItem(this)
    }
}