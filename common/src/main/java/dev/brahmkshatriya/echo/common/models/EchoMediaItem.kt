package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable
import java.io.Serializable as JSerializable


sealed class EchoMediaItem : JSerializable {

    @Serializable
    data class TrackItem(val track: Track) : EchoMediaItem()

    @Serializable
    sealed class Profile : EchoMediaItem() {
        data class ArtistItem(val artist: Artist) : Profile()
        data class UserItem(val user: User) : Profile()
    }

    @Serializable
    sealed class Lists : EchoMediaItem() {
        data class AlbumItem(val album: Album) : Lists()
        data class PlaylistItem(val playlist: Playlist) : Lists()

        val size
            get() = when (this) {
                is AlbumItem -> album.tracks
                is PlaylistItem -> playlist.tracks
            }
    }

    companion object {
        fun Track.toMediaItem() = TrackItem(this)
        fun Album.toMediaItem() = Lists.AlbumItem(this)
        fun Artist.toMediaItem() = Profile.ArtistItem(this)
        fun User.toMediaItem() = Profile.UserItem(this)
        fun Playlist.toMediaItem() = Lists.PlaylistItem(this)

//        val creator = object : Parcelable.Creator<EchoMediaItem> {
//
//            inline fun <reified T : Parcelable> create(source: Parcel?) = runCatching {
//                parcelableCreator<T>().createFromParcel(source)!!
//            }.getOrNull()
//
//            override fun createFromParcel(source: Parcel?): EchoMediaItem {
//                return create<Lists.AlbumItem>(source)
//                    ?: create<Lists.PlaylistItem>(source)
//                    ?: create<TrackItem>(source)
//                    ?: create<Profile.ArtistItem>(source)
//                    ?: create<Profile.UserItem>(source)
//                    ?: throw IllegalArgumentException("Unknown parcelable type")
//            }
//
//            override fun newArray(size: Int): Array<EchoMediaItem?> {
//                return arrayOfNulls(size)
//            }
//
//        }
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