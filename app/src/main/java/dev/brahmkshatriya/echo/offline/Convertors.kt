package dev.brahmkshatriya.echo.offline

import androidx.media3.common.MediaItem
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toUriImageHolder
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.offline.MediaStoreUtils.id
import dev.brahmkshatriya.echo.offline.MediaStoreUtils.splitArtists

fun MediaItem.toTrack() = mediaMetadata.run {
    val artists = artist.toString().takeIf { it != "null" }.splitArtists().map {
        Artist(it?.id().toString(), it ?: "Unknown")
    }
    val album = Album(extras?.getLong("AlbumId").toString(), albumTitle.toString())
    val liked = extras?.getBoolean("Liked") ?: false
    Track(
        mediaId,
        title.toString(),
        artists,
        album,
        artworkUri.toString().toUriImageHolder(),
        extras?.getLong("Duration"),
        null,
        releaseYear.toString(),
        liked,
        mapOf(),
        listOf(),
        listOf(Streamable(localConfiguration!!.uri.toString(), 0))
    )
}

fun MediaStoreUtils.MAlbum.toAlbum() = Album(
    id.toString(),
    title ?: "Unknown",
    cover.toString().toUriImageHolder(),
    artists.map { it.toArtist() },
    songList.size,
    albumYear?.toString()
)

fun MediaStoreUtils.MArtist?.toArtist() = Artist(
    this?.id.toString(),
    this?.title ?: "Unknown",
    this?.songList?.firstOrNull()?.toTrack()?.cover
)

fun MediaStoreUtils.MPlaylist.toPlaylist() = Playlist(
    id.toString(),
    title ?: "Unknown",
    true,
    songList.firstOrNull()?.toTrack()?.cover,
    listOf(),
    songList.size,
    "Modified " + modifiedDate.toTimeAgo(),
    songList.sumOf { it.mediaMetadata.extras?.getLong("Duration") ?: 0 },
    description
)

private fun Long.toTimeAgo() = when (val diff = System.currentTimeMillis() / 1000 - this) {
    in 0..59 -> "Just now"
    in 60..3599 -> "${diff / 60}min ago"
    in 3600..86399 -> "${diff / 3600}h ago"
    in 86400..2591999 -> "${diff / 86400}d ago"
    in 2592000..31535999 -> "${diff / 2592000}m ago"
    else -> "${diff / 31536000}y ago"
}

fun MediaStoreUtils.FileNode.toContainer(title: String?): MediaItemsContainer.Container = run {
    if (folderList.size == 1 && songList.isEmpty())
        return@run folderList.entries.first()
            .run { value.toContainer("${title ?: folderName}/$key") }
    val itemSize = folderList.size + songList.size
    MediaItemsContainer.Container(
        title ?: folderName,
        "$itemSize Items",
        PagedData.Single {
            folderList.map {
                it.value.toContainer(it.key)
            } + songList.map {
                it.toTrack().toMediaItem().toMediaItemsContainer()
            }
        }
    )
}

fun MediaStoreUtils.Genre.toContainer(): MediaItemsContainer = MediaItemsContainer.Category(
    title ?: "Unknown",
    songList.map { it.toTrack().toMediaItem() }.take(10),
    more = PagedData.Single {
        songList.map { it.toTrack().toMediaItem() }
    }
)
