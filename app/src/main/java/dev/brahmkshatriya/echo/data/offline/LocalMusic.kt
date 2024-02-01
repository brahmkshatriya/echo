package dev.brahmkshatriya.echo.data.offline

import android.content.Context
import android.net.Uri
import dev.brahmkshatriya.echo.data.models.Album
import dev.brahmkshatriya.echo.data.models.Artist
import dev.brahmkshatriya.echo.data.models.Track
import dev.brahmkshatriya.echo.data.models.toFileUrl

const val URI = "local://"
const val ARTIST_AUTH = "artist/"
const val ALBUM_AUTH = "album/"
const val TRACK_AUTH = "track/"

fun Context.searchLocally(query: String, page: Int, pageSize: Int): List<Track.Small> {
    val tracks = mutableListOf<Track.Small>()
    val cursor = contentResolver.query(
        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        arrayOf(
            android.provider.MediaStore.Audio.Media._ID,
            android.provider.MediaStore.Audio.Media.TITLE,
            android.provider.MediaStore.Audio.Media.ARTIST,
            android.provider.MediaStore.Audio.Media.ALBUM,
            android.provider.MediaStore.Audio.Media.ALBUM_ID,
            android.provider.MediaStore.Audio.Media.DURATION,
            android.provider.MediaStore.Audio.Media.YEAR,
        ),
        "${android.provider.MediaStore.Audio.Media.TITLE} LIKE ? OR ${android.provider.MediaStore.Audio.Media.ARTIST} LIKE ?",
        arrayOf("%$query%", "%$query%"),
        "${android.provider.MediaStore.Audio.Media.TITLE} ASC LIMIT $pageSize OFFSET $page"
    )

    cursor?.use {
        //Cache column indices.
        val idColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
        val titleColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
        val artistIdColumn =
            it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST_ID)
        val artistColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
        val albumColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM)
        val albumIdColumn =
            it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM_ID)
        val durationColumn =
            it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)

        while (it.moveToNext()) {
            val uri = Uri.parse("$URI$TRACK_AUTH${it.getLong(idColumn)}")
            val coverArt = android.content.ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                it.getLong(albumIdColumn)
            ).path.toFileUrl()
            val artistUri = Uri.parse("$URI$ARTIST_AUTH${it.getLong(artistIdColumn)}")
            val albumUri = Uri.parse("$URI$ALBUM_AUTH${it.getLong(albumIdColumn)}")
            tracks.add(
                Track.Small(
                    uri = uri,
                    title = it.getString(titleColumn),
                    artists = listOf(Artist.Small(artistUri, it.getString(artistColumn))),
                    album = Album.Small(albumUri, it.getString(albumColumn)),
                    cover = coverArt,
                    duration = it.getLong(durationColumn),
                    releaseDate = it.getString(it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.YEAR)),
                    plays = null,
                    liked = false,
                )
            )
        }
    }
    return tracks
}