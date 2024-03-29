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

    fun toMediaItemsContainer() = MediaItemsContainer.Item(
        when (this) {
            is TrackItem -> track.toMediaItem()
            is AlbumItem -> album.toMediaItem()
            is ArtistItem -> artist.toMediaItem()
            is PlaylistItem -> playlist.toMediaItem()
        }
    )

    fun sameAs(other: EchoMediaItem) = when (this) {
        is TrackItem -> other is TrackItem && track.id == other.track.id
        is AlbumItem -> other is AlbumItem && album.id == other.album.id
        is ArtistItem -> other is ArtistItem && artist.id == other.artist.id
        is PlaylistItem -> other is PlaylistItem && playlist.id == other.playlist.id
    }

    val id
        get() = when (this) {
            is TrackItem -> track.id
            is AlbumItem -> album.id
            is ArtistItem -> artist.id
            is PlaylistItem -> playlist.id
        }

    val title
        get() = when (this) {
            is TrackItem -> track.title
            is AlbumItem -> album.title
            is ArtistItem -> artist.name
            is PlaylistItem -> playlist.title
        }

    val cover
        get() = when (this) {
            is TrackItem -> track.cover
            is AlbumItem -> album.cover
            is ArtistItem -> artist.cover
            is PlaylistItem -> playlist.cover
        }

    val subtitle
        get() = when (this) {
            is TrackItem -> track.artists.joinToString(", "){ it.name }
            is AlbumItem -> album.subtitle ?: album.artists.joinToString(", "){ it.name }
            is ArtistItem -> artist.subtitle
            is PlaylistItem -> playlist.subtitle
        }
}