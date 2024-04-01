package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import dev.brahmkshatriya.echo.common.helpers.PagedData
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class EchoMediaItem : Parcelable {
    data class TrackItem(val track: Track) : EchoMediaItem()

    @Parcelize
    sealed class Profile : EchoMediaItem() {
        data class ArtistItem(val artist: Artist) : Profile()
        data class UserItem(val user: User) : Profile()
    }

    @Parcelize
    sealed class Lists : EchoMediaItem() {
        data class AlbumItem(val album: Album) : Lists()
        data class PlaylistItem(val playlist: Playlist) : Lists()

        val tracks
            get() = when (this) {
                is AlbumItem -> album.tracks
                is PlaylistItem -> playlist.tracks
            }

        val size
            get() = when (this) {
                is AlbumItem -> album.numberOfTracks ?: album.tracks.ifEmpty { null }?.size
                is PlaylistItem -> playlist.tracks.ifEmpty { null }?.size
            }
    }

    companion object {
        fun Track.toMediaItem() = TrackItem(this)
        fun Album.toMediaItem() = Lists.AlbumItem(this)
        fun Artist.toMediaItem() = Profile.ArtistItem(this)
        fun User.toMediaItem() = Profile.UserItem(this)

        fun Playlist.toMediaItem() = Lists.PlaylistItem(this)


        fun List<EchoMediaItem>.toMediaItemsContainer(
            title: String, subtitle: String? = null, more: PagedData<EchoMediaItem>? = null
        ) = MediaItemsContainer.Category(title, this, subtitle, more)

    }

    fun toMediaItemsContainer() = MediaItemsContainer.Item(
        when (this) {
            is TrackItem -> track.toMediaItem()
            is Profile.ArtistItem -> artist.toMediaItem()
            is Profile.UserItem -> user.toMediaItem()
            is Lists.AlbumItem -> album.toMediaItem()
            is Lists.PlaylistItem -> playlist.toMediaItem()
        }
    )

    fun sameAs(other: EchoMediaItem) = when (this) {
        is TrackItem -> other is TrackItem && track.id == other.track.id
        is Profile.ArtistItem -> other is Profile.ArtistItem && artist.id == other.artist.id
        is Profile.UserItem -> other is Profile.UserItem && user.id == other.user.id
        is Lists.AlbumItem -> other is Lists.AlbumItem && album.id == other.album.id
        is Lists.PlaylistItem -> other is Lists.PlaylistItem && playlist.id == other.playlist.id
    }

    val id
        get() = when (this) {
            is TrackItem -> track.id
            is Profile.ArtistItem -> artist.id
            is Profile.UserItem -> user.id
            is Lists.AlbumItem -> album.id
            is Lists.PlaylistItem -> playlist.id
        }

    val title
        get() = when (this) {
            is TrackItem -> track.title
            is Profile.ArtistItem -> artist.name
            is Profile.UserItem -> user.name
            is Lists.AlbumItem -> album.title
            is Lists.PlaylistItem -> playlist.title
        }

    val cover
        get() = when (this) {
            is TrackItem -> track.cover
            is Profile.ArtistItem -> artist.cover
            is Profile.UserItem -> user.cover
            is Lists.AlbumItem -> album.cover
            is Lists.PlaylistItem -> playlist.cover
        }

    val subtitle
        get() = when (this) {
            is TrackItem -> track.artists.joinToString(", ") { it.name }
            is Profile.ArtistItem -> artist.subtitle
            is Profile.UserItem -> null
            is Lists.AlbumItem -> album.subtitle ?: album.artists.joinToString(", ") { it.name }
            is Lists.PlaylistItem -> playlist.subtitle
        }
}