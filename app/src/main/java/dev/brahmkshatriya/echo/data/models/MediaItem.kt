package dev.brahmkshatriya.echo.data.models

sealed class MediaItem {
    data class TrackItem(val track: Track.Small) : MediaItem()
    data class AlbumItem(val album: Album.WithCover) : MediaItem()
    data class ArtistItem(val artist: Artist.WithCover) : MediaItem()
    data class PlaylistItem(val playlist: Playlist.WithCover) : MediaItem()
}

fun Track.Small.toMediaItem() = MediaItem.TrackItem(this)
fun Album.WithCover.toMediaItem() = MediaItem.AlbumItem(this)
fun Artist.WithCover.toMediaItem() = MediaItem.ArtistItem(this)
fun Playlist.WithCover.toMediaItem() = MediaItem.PlaylistItem(this)