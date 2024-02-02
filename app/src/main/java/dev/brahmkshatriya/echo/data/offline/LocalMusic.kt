package dev.brahmkshatriya.echo.data.offline

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import dev.brahmkshatriya.echo.data.models.Album
import dev.brahmkshatriya.echo.data.models.Artist
import dev.brahmkshatriya.echo.data.models.Track
import dev.brahmkshatriya.echo.data.models.toImageHolder


const val URI = "local://"
const val ARTIST_AUTH = "artist/"
const val ALBUM_AUTH = "album/"
const val TRACK_AUTH = "track/"

fun Context.searchTracksLocally(query: String, page: Int, pageSize: Int): List<Track> {

    val whereCondition =
        "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ? OR ${MediaStore.Audio.Media.ALBUM} LIKE ?"
    val selectionArgs = arrayOf("%$query%", "%$query%", "%$query%")

    val tracks = mutableListOf<Track>()
    createCursor(
        contentResolver = contentResolver,
        collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.YEAR,
        ),
        whereCondition = whereCondition,
        selectionArgs = selectionArgs,
        orderBy = "ALPHABET",
        orderAscending = true,
        limit = pageSize,
        offset = (page - 1) * pageSize
    )?.use {
        //Cache column indices.
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistIdColumn =
            it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
        val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val albumIdColumn =
            it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val durationColumn =
            it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

        while (it.moveToNext()) {
            val uri = Uri.parse("$URI$TRACK_AUTH${it.getLong(idColumn)}")
            val sArtworkUri = Uri
                .parse("content://media/external/audio/albumart")
            val coverUri = ContentUris.withAppendedId(
                sArtworkUri,
                it.getLong(albumIdColumn)
            )

            val artistUri = Uri.parse("$URI$ARTIST_AUTH${it.getLong(artistIdColumn)}")
            val albumUri = Uri.parse("$URI$ALBUM_AUTH${it.getLong(albumIdColumn)}")
            tracks.add(
                Track(
                    uri = uri,
                    title = it.getString(titleColumn),
                    artists = listOf(Artist.Small(artistUri, it.getString(artistColumn))),
                    album = Album.Small(albumUri, it.getString(albumColumn)),
                    cover = coverUri.toImageHolder(),
                    duration = it.getLong(durationColumn),
                    releaseDate = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)),
                    plays = null,
                    liked = false,
                )
            )
        }
    }
    return tracks
}

fun Context.searchArtistsLocally(query: String, page: Int, pageSize: Int): List<Artist.WithCover> {
    val artists = mutableListOf<Artist.WithCover>()
    val whereCondition = "${MediaStore.Audio.Artists.ARTIST} LIKE ?"
    val selectionArgs = arrayOf("%$query%")

    createCursor(
        contentResolver = contentResolver,
        collection = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
        projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST
        ),
        whereCondition = whereCondition,
        selectionArgs = selectionArgs,
        orderBy = "ALPHABET",
        orderAscending = true,
        limit = pageSize,
        offset = (page - 1) * pageSize
    )?.use {
        //Cache column indices.
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
        val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)

        while (it.moveToNext()) {
            val uri = Uri.parse("$URI$ARTIST_AUTH${it.getLong(idColumn)}")
            artists.add(
                Artist.WithCover(
                    uri = uri,
                    name = it.getString(artistColumn),
                    cover = null,
                )
            )
        }
    }
    return artists
}

fun Context.searchAlbumsLocally(query: String, page: Int, pageSize: Int): List<Album.WithCover> {
    val albums = mutableListOf<Album.WithCover>()
    val whereCondition = "${MediaStore.Audio.Albums.ALBUM} LIKE ?"
    val selectionArgs = arrayOf("%$query%")

    createCursor(
        contentResolver = contentResolver,
        collection = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
        projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST
        ),
        whereCondition = whereCondition,
        selectionArgs = selectionArgs,
        orderBy = "ALPHABET",
        orderAscending = true,
        limit = pageSize,
        offset = (page - 1) * pageSize
    )?.use {
        //Cache column indices.
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
        val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
        val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
        while (it.moveToNext()) {
            val uri = Uri.parse("$URI$ALBUM_AUTH${it.getLong(idColumn)}")
            val coverUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                it.getLong(idColumn)
            )
            val artistUri = Uri.parse("$URI$ARTIST_AUTH${it.getLong(idColumn)}")
            albums.add(
                Album.WithCover(
                    uri = uri,
                    title = it.getString(albumColumn),
                    cover = coverUri.toImageHolder(),
                    artists = listOf(Artist.Small(artistUri, it.getString(artistColumn))),
                )
            )
        }
    }
    return albums
}

fun createCursor(
    contentResolver: ContentResolver,
    collection: Uri,
    projection: Array<String>,
    whereCondition: String,
    selectionArgs: Array<String>,
    orderBy: String,
    orderAscending: Boolean,
    limit: Int = 10,
    offset: Int = 0
): Cursor? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
        val selection = createSelectionBundle(
            whereCondition,
            selectionArgs,
            orderBy,
            orderAscending,
            limit,
            offset
        )
        contentResolver.query(collection, projection, selection, null)
    }

    else -> {
        val orderDirection = if (orderAscending) "ASC" else "DESC"
        var order = when (orderBy) {
            "ALPHABET" -> "${MediaStore.Audio.Media.TITLE}, ${MediaStore.Audio.Media.ARTIST} $orderDirection"
            else -> "${MediaStore.Audio.Media.DATE_ADDED} $orderDirection"
        }
        order += " LIMIT $limit OFFSET $offset"
        contentResolver.query(collection, projection, whereCondition, selectionArgs, order)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun createSelectionBundle(
    whereCondition: String,
    selectionArgs: Array<String>,
    orderBy: String,
    orderAscending: Boolean,
    limit: Int = 10,
    offset: Int = 0
): Bundle = Bundle().apply {
    // Limit & Offset
    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
    when (orderBy) {
        "ALPHABET" -> putStringArray(
            ContentResolver.QUERY_ARG_SORT_COLUMNS,
            arrayOf(MediaStore.Files.FileColumns.TITLE)
        )

        else -> putStringArray(
            ContentResolver.QUERY_ARG_SORT_COLUMNS,
            arrayOf(MediaStore.Files.FileColumns.DATE_ADDED)
        )
    }
    val orderDirection = if (orderAscending)
        ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
    else
        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
    putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, orderDirection)
    // Selection
    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, whereCondition)
    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
}