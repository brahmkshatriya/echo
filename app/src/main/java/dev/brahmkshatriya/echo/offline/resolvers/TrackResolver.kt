package dev.brahmkshatriya.echo.offline.resolvers

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.StreamableAudio.Companion.toAudio
import dev.brahmkshatriya.echo.common.models.Track
import java.io.File

class TrackResolver(val context: Context) {

    private fun order(sorting: String) = when (sorting) {
        "date" -> MediaStore.Audio.Media.TRACK
        "a_to_z" -> MediaStore.Audio.Media.TITLE
        "z_to_a" -> MediaStore.Audio.Media.TITLE
        "year" -> MediaStore.Audio.Media.YEAR
        else -> MediaStore.Audio.Media.TITLE
    }

    private fun isAscending(sorting: String) = when (sorting) {
        "a_to_z" -> true
        "z_to_a" -> false
        else -> true
    }

    fun search(query: String, page: Int, pageSize: Int, sorting: String): List<Track> {
        val whereCondition =
            "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ? OR ${MediaStore.Audio.Media.ALBUM} LIKE ?"
        val selectionArgs = arrayOf("%$query%", "%$query%", "%$query%")

        return context.queryTracks(whereCondition, selectionArgs, page, pageSize, sorting)
            .sortedBy(query) {
                it.title
            }
    }

    fun getAll(page: Int, pageSize: Int, sorting: String): List<Track> {
        val whereCondition = ""
        val selectionArgs = arrayOf<String>()
        return context.queryTracks(whereCondition, selectionArgs, page, pageSize, sorting)
    }

    fun getShuffled(pageSize: Int): List<Track> {
        return getAll(0, 100, "").shuffled().take(pageSize)
    }

    fun getByArtist(
        artist: Artist, page: Int, pageSize: Int, sorting: String
    ): List<Track> {
        val name = artist.name
        return search(name, page, pageSize, sorting)
    }

    fun getByAlbum(
        album: Album, page: Int, pageSize: Int, sorting: String
    ): List<Track> {
        val whereCondition = "${MediaStore.Audio.Media.ALBUM_ID} = ?"
        val selectionArgs = arrayOf(album.id.toUri().lastPathSegment!!)
        return context.queryTracks(whereCondition, selectionArgs, page, pageSize, sorting)
    }

    private fun Context.queryTracks(
        whereCondition: String,
        selectionArgs: Array<String>,
        page: Int,
        pageSize: Int,
        sorting: String
    ): List<Track> {
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
            orderBy = order(sorting),
            orderAscending = isAscending(sorting),
            limit = pageSize,
            offset = page * pageSize
        )?.use {
            //Cache column indices.
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val releaseDateColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val uri = "$URI$TRACK_AUTH${id}"
                val albumId = it.getLong(albumIdColumn)
                val coverUri = ContentUris.withAppendedId(
                    ARTWORK_URI, albumId
                )

                val artistUri = "$URI$ARTIST_AUTH${it.getLong(artistIdColumn)}"
                val albumUri = "$URI$ALBUM_AUTH${albumId}"
                tracks.add(
                    Track(
                        id = uri,
                        title = it.getString(titleColumn),
                        audioStreamables = listOf(Streamable(uri, 0)),
                        artists = listOf(
                            Artist(
                                artistUri,
                                it.getStringOrNull(artistColumn) ?: "PROBLEM CHILD"
                            )
                        ),
                        album = Album(
                            albumUri,
                            it.getStringOrNull(albumColumn) ?: "PROBLEM CHILD"
                        ),
                        cover = coverUri.toImageHolder(),
                        duration = it.getLongOrNull(durationColumn),
                        releaseDate = it.getStringOrNull(releaseDateColumn),
                        plays = null,
                        liked = false,
                    )
                )
            }
        }
        return tracks
    }

    fun get(uri: Uri): Track? {
        val id = uri.lastPathSegment ?: return null
        val whereCondition = "${MediaStore.Audio.Media._ID} = ?"
        val selectionArgs = arrayOf(id)
        return context.queryTracks(whereCondition, selectionArgs, 0, 1, "").firstOrNull()
    }

    fun getStreamable(stream: Streamable): StreamableAudio {
        val id = stream.id.toUri().lastPathSegment!!
        return ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            id.toLong(),
        ).toAudio()
    }

    fun fromFile(file: File): Track? {
        return context.queryTracks(
            whereCondition = "${MediaStore.Audio.Media.DATA} = ?",
            selectionArgs = arrayOf(file.path),
            page = 0,
            pageSize = 1,
            sorting = ""
        ).firstOrNull()
    }
}