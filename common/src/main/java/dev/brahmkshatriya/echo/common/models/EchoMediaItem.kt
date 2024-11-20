package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@Serializable
sealed class EchoMediaItem {

    @Serializable
    data class TrackItem(val track: Track) : EchoMediaItem()

    @Serializable
    sealed class Profile : EchoMediaItem() {
        @Serializable
        data class ArtistItem(val artist: Artist) : Profile()

        @Serializable
        data class UserItem(val user: User) : Profile()
    }

    @Serializable
    sealed class Lists : EchoMediaItem() {
        @Serializable
        data class AlbumItem(val album: Album) : Lists()

        @Serializable
        data class PlaylistItem(val playlist: Playlist) : Lists()

        @Serializable
        data class RadioItem(val radio: Radio) : Lists()

        val size
            get() = when (this) {
                is AlbumItem -> album.tracks
                is PlaylistItem -> playlist.tracks
                is RadioItem -> 0
            }
    }

    companion object {
        fun Track.toMediaItem() = TrackItem(this)
        fun Album.toMediaItem() = Lists.AlbumItem(this)
        fun Artist.toMediaItem() = Profile.ArtistItem(this)
        fun User.toMediaItem() = Profile.UserItem(this)
        fun Playlist.toMediaItem() = Lists.PlaylistItem(this)
        fun Radio.toMediaItem() = Lists.RadioItem(this)
    }

    fun toShelf(loadTrack: Boolean = false) = Shelf.Item(
        when (this) {
            is TrackItem -> track.toMediaItem()
            is Profile.ArtistItem -> artist.toMediaItem()
            is Profile.UserItem -> user.toMediaItem()
            is Lists.AlbumItem -> album.toMediaItem()
            is Lists.PlaylistItem -> playlist.toMediaItem()
            is Lists.RadioItem -> radio.toMediaItem()
        },
        loadTrack
    )

    fun sameAs(other: EchoMediaItem) = when (this) {
        is TrackItem -> other is TrackItem && track.id == other.track.id
        is Profile.ArtistItem -> other is Profile.ArtistItem && artist.id == other.artist.id
        is Profile.UserItem -> other is Profile.UserItem && user.id == other.user.id
        is Lists.AlbumItem -> other is Lists.AlbumItem && album.id == other.album.id
        is Lists.PlaylistItem -> other is Lists.PlaylistItem && playlist.id == other.playlist.id
        is Lists.RadioItem -> other is Lists.RadioItem && radio.id == other.radio.id
    }

    val id
        get() = when (this) {
            is TrackItem -> track.id
            is Profile.ArtistItem -> artist.id
            is Profile.UserItem -> user.id
            is Lists.AlbumItem -> album.id
            is Lists.PlaylistItem -> playlist.id
            is Lists.RadioItem -> radio.id
        }

    val title
        get() = when (this) {
            is TrackItem -> track.title
            is Profile.ArtistItem -> artist.name
            is Profile.UserItem -> user.name
            is Lists.AlbumItem -> album.title
            is Lists.PlaylistItem -> playlist.title
            is Lists.RadioItem -> radio.title
        }

    val cover
        get() = when (this) {
            is TrackItem -> track.cover
            is Profile.ArtistItem -> artist.cover
            is Profile.UserItem -> user.cover
            is Lists.AlbumItem -> album.cover
            is Lists.PlaylistItem -> playlist.cover
            is Lists.RadioItem -> radio.cover
        }

    val subtitle
        get() = when (this) {
            is TrackItem -> track.run { subtitle ?: artists.joinToString(", ") { it.name } }
            is Profile.ArtistItem -> artist.subtitle
            is Profile.UserItem -> null
            is Lists.AlbumItem -> album.run { subtitle ?: artists.joinToString(", ") { it.name } }
            is Lists.RadioItem -> radio.subtitle
            is Lists.PlaylistItem -> playlist.run {
                subtitle ?: authors.joinToString(", ") { it.name }
            }
        }

    val extras
        get() = when (this) {
            is TrackItem -> track.extras
            is Profile.ArtistItem -> artist.extras
            is Profile.UserItem -> user.extras
            is Lists.AlbumItem -> album.extras
            is Lists.PlaylistItem -> playlist.extras
            is Lists.RadioItem -> radio.extras
        }
}