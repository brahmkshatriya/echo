package dev.brahmkshatriya.echo.extensions.builtin.offline

import android.content.Context
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Date
import dev.brahmkshatriya.echo.common.models.Date.Companion.toDate
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toUriImageHolder
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension.Companion.EXTENSION_ID

fun MediaStoreUtils.MAlbum.toAlbum() = Album(
    id.toString(),
    title ?: "Unknown",
    cover.toString().toUriImageHolder(),
    artists.map { it.toArtist() },
    songList.size,
    songList.sumOf { it.duration ?: 0 },
    albumYear?.toDate(),
    extras = mapOf(EXTENSION_ID to OfflineExtension.metadata.id)
)

fun MediaStoreUtils.MArtist?.toArtist() = Artist(
    this?.id.toString(),
    this?.title ?: "Unknown",
    this?.songList?.firstOrNull()?.cover,
    extras = mapOf(EXTENSION_ID to OfflineExtension.metadata.id)
)

fun MediaStoreUtils.MPlaylist.toPlaylist() = Playlist(
    id.toString(),
    title ?: "Unknown",
    true,
    songList.firstOrNull()?.cover,
    listOf(),
    songList.size,
    songList.sumOf { it.duration ?: 0 },
    modifiedDate.toDate(),
    description,
    extras = mapOf(EXTENSION_ID to OfflineExtension.metadata.id)
)

//coverts epoch seconds long to Date
fun Long.toDate() = run {
    val year = (this / 31556952 + 1970).toInt()
    val month = (this % 31556952 / 2629746).toInt()
    val day = ((this % 31556952) % 2629746 / 86400).toInt()
    Date(year, month, day)
}

fun MediaStoreUtils.FileNode.toShelf(
    context: Context,
    title: String?
): Shelf.Category = run {
    if (folderList.size == 1 && songList.isEmpty())
        return@run folderList.entries.first()
            .run { value.toShelf(context, "${title ?: folderName}/$key") }
    val itemSize = folderList.size + songList.size
    Shelf.Category(
        title ?: folderName,
        PagedData.Single {
            folderList.map {
                it.value.toShelf(context, it.key)
            } + songList.map {
                it.toMediaItem().toShelf()
            }
        },
        context.getString(R.string.n_items, itemSize),
    )
}

fun MediaStoreUtils.Genre.toShelf(): Shelf = Shelf.Lists.Items(
    title ?: "Unknown",
    songList.map { it.toMediaItem() }.take(10),
    more = PagedData.Single {
        songList.map { it.toMediaItem() }
    },
)
