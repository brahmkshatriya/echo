package dev.brahmkshatriya.echo.extensions.builtin.offline

import android.content.Context
import android.util.Log
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.model.synced.UncheckedSyncedLine
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Date
import dev.brahmkshatriya.echo.common.models.Date.Companion.toYearDate
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceImageHolder
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceUriImageHolder
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension.Companion.EXTENSION_ID

fun MediaStoreUtils.MAlbum.toAlbum() = Album(
    id.toString(),
    title ?: "Unknown",
    null,
    cover.toString().toResourceUriImageHolder(),
    artists.map { it.toArtist() },
    songList.size.toLong(),
    songList.sumOf { it.duration ?: 0 },
    albumYear?.toYearDate(),
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
    isPrivate = true,
    cover = songList.firstOrNull()?.cover,
    authors = listOf(),
    trackCount = songList.size.toLong(),
    duration = songList.sumOf { it.duration ?: 0 },
    creationDate = modifiedDate.toDate(),
    description = description,
    extras = mapOf(EXTENSION_ID to OfflineExtension.metadata.id)
)

fun Long.toDate() = Date(this)

private fun MediaStoreUtils.FileNode.toSongList(): List<Track> =
    songList + folderList.values.flatMap { it.toSongList() }

fun MediaStoreUtils.FileNode.toShelf(
    context: Context,
    title: String?
): Shelf.Category = run {
    if (folderList.size == 1 && songList.isEmpty())
        return@run folderList.entries.first()
            .run { value.toShelf(context, "${title ?: folderName}/$key") }
    val itemSize = folderList.size + songList.size
    Shelf.Category(
        folderName,
        title ?: folderName,
        PagedData.Single {
            folderList.map {
                it.value.toShelf(context, it.key)
            } + songList.map { it.toShelf() }
        }.toFeed(
            Feed.Buttons(
                showPlayAndShuffle = true,
                customTrackList = toSongList()
            ),
            songList.firstOrNull()?.cover
        ),
        context.resources.runCatching {
            getQuantityString(R.plurals.number_items, itemSize, itemSize)
        }.getOrNull() ?: context.getString(R.string.n_items, itemSize),
        image = songList.firstOrNull()?.cover ?: R.drawable.ic_offline_files.toResourceImageHolder()
    )
}

fun MediaStoreUtils.Genre.toShelf(): Shelf {
    val id = id.toString()
    return Shelf.Lists.Tracks(
        id,
        title ?: "Unknown",
        songList.take(9),
        more = PagedData.Single<Shelf> { songList.map { it.toShelf() } }.toFeed()
    )
}

fun SyncedLyrics.toEchoLyrics(id: String, title: String): Lyrics {
    Log.i("[SyncedLyrics.toEchoLyrics]", "Converting lyrics for id=$id")
    var isKaraoke = false
    val transformedLyrics = this.lines.map {
        return@map when (it) {
            is SyncedLine -> {
//                Log.i("[SyncedLyrics.toEchoLyrics]", "Converting SyncedLine to Lyrics.Item { start = ${it.start}, end = ${it.end}, content = ${it.content} }")
                listOf(Lyrics.Item(
                    it.content,
                    fixTime(it.start),
                    fixTime(it.end)))
            }
            is UncheckedSyncedLine -> {
//                Log.i("[SyncedLyrics.toEchoLyrics]", "Converting UncheckedSyncedLine to Lyrics.Item { start = ${it.start}, end = ${it.end}, content = ${it.content} }")

                listOf(Lyrics.Item(it.content, fixTime(it.start), fixTime(it.end)))
            }
            is KaraokeLine -> {
//                Log.i("[SyncedLyrics.toEchoLyrics]", "Converting KaraokeLine to List<Lyrics.Item>")

                isKaraoke = true
                it.syllables.map { syl ->
                    Lyrics.Item(syl.content, syl.start.toLong(), syl.end.toLong())
                }
            }
            else -> {
                Log.e("[SyncedLyrics.toEchoLyrics]", "Lyric line kind not handled: ${it::class.qualifiedName}")
                emptyList()
            }
        }
    }
    val lyric = if (isKaraoke) {
        Lyrics.WordByWord(
            transformedLyrics
        )
    } else {
        Lyrics.Timed(
            transformedLyrics.flatten()
        )
    }
    Log.i("[SyncedLyrics.toEchoLyrics]", "When converting, isKaraoke=$isKaraoke, lyric::class=${lyric::class.simpleName}")

    return Lyrics(
        id, title,
        lyrics = lyric
    )
}
private fun fixTime(time: Int): Long {
    return if (time < 1000) (time.toLong() * 1000) else time.toLong()
}