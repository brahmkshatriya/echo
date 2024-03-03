package dev.brahmkshatriya.echo.data.offline

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.Track

class TrackResolver(val context: Context) {
    fun search(query: String, page: Int, pageSize: Int): List<Track> {
        val whereCondition =
            "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ? OR ${MediaStore.Audio.Media.ALBUM} LIKE ?"
        val selectionArgs = arrayOf("%$query%", "%$query%", "%$query%")

        return context.queryTracks(whereCondition, selectionArgs, page, pageSize).sortedBy(query) {
            it.title
        }
    }

    fun getAll(page: Int, pageSize: Int): List<Track> {
        val whereCondition = ""
        val selectionArgs = arrayOf<String>()
        return context.queryTracks(whereCondition, selectionArgs, page, pageSize)
    }

    fun getShuffled(page: Int, pageSize: Int): List<Track> {
        val whereCondition = ""
        val selectionArgs = arrayOf<String>()
        return context.queryTracks(whereCondition, selectionArgs, page, pageSize).shuffled()
    }

    fun getByArtist(
        artist: Artist.Small, page: Int, pageSize: Int
    ): List<Track> {
        val whereCondition = "${MediaStore.Audio.Media.ARTIST_ID} = ?"
        val selectionArgs = arrayOf(artist.uri.lastPathSegment!!)
        return context.queryTracks(whereCondition, selectionArgs, page, pageSize)
    }

    fun getByAlbum(
        album: Album.Small, page: Int, pageSize: Int
    ): List<Track> {
        val whereCondition = "${MediaStore.Audio.Media.ALBUM_ID} = ?"
        val selectionArgs = arrayOf(album.uri.lastPathSegment!!)
        return context.queryTracks(whereCondition, selectionArgs, page, pageSize)
    }

    private fun Context.queryTracks(
        whereCondition: String, selectionArgs: Array<String>, page: Int, pageSize: Int
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
            orderBy = MediaStore.Audio.Media.TRACK,
            orderAscending = true,
            limit = pageSize,
            offset = (page - 1) * pageSize
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
                val uri = Uri.parse("$URI$TRACK_AUTH${it.getLong(idColumn)}")
                val coverUri = ContentUris.withAppendedId(
                    ARTWORK_URI, it.getLong(albumIdColumn)
                )

                val artistUri = Uri.parse("$URI$ARTIST_AUTH${it.getLong(artistIdColumn)}")
                val albumUri = Uri.parse("$URI$ALBUM_AUTH${it.getLong(albumIdColumn)}")
                tracks.add(
                    Track(
                        uri = uri,
                        title = it.getString(titleColumn),
                        artists = listOf(
                            Artist.Small(
                                artistUri,
                                it.getStringOrNull(artistColumn) ?: "PROBLEM CHILD"
                            )
                        ),
                        album = Album.Small(
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
        return context.queryTracks(whereCondition, selectionArgs, 0, 1).firstOrNull()
    }

    fun getStream(track: Track): Uri {
        val id = track.uri.lastPathSegment ?: throw IllegalArgumentException("Invalid track uri")
        return ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            id.toLong(),
        )
    }

}