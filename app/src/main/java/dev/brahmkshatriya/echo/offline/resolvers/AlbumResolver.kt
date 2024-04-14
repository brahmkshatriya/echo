package dev.brahmkshatriya.echo.offline.resolvers

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder

class AlbumResolver(
    val context: Context,
) {

    private fun order() = MediaStore.Audio.Albums.ALBUM
    private fun isAscending(sorting: String) = when (sorting) {
        "a_to_z" -> true
        "z_to_a" -> false
        else -> true
    }

    fun search(
        query: String, page: Int, pageSize: Int, sorting: String
    ): List<Album> {
        val whereCondition =
            "${MediaStore.Audio.Media.ARTIST} LIKE ? OR ${MediaStore.Audio.Media.ALBUM} LIKE ?"
        val selectionArgs = arrayOf("%$query%", "%$query%")
        return context.queryAlbums(whereCondition, selectionArgs, page, pageSize, sorting)
            .sortedBy(query) {
                it.title
            }
    }

    fun getAll(page: Int, pageSize: Int, sorting: String): List<Album> {
        val whereCondition = ""
        val selectionArgs = arrayOf<String>()
        return context.queryAlbums(whereCondition, selectionArgs, page, pageSize, sorting)
    }

    fun getByArtist(
        artist: Artist, page: Int, pageSize: Int, sorting: String
    ): List<Album> {
        val name = artist.name
        return search(name, page, pageSize, sorting)
    }

    private fun Context.queryAlbums(
        whereCondition: String,
        selectionArgs: Array<String>,
        page: Int,
        pageSize: Int,
        sorting: String
    ): MutableList<Album> {
        val albums = mutableListOf<Album>()
        createCursor(
            contentResolver = contentResolver,
            collection = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            projection = arrayOf(
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Albums.NUMBER_OF_SONGS,
                MediaStore.Audio.Albums.FIRST_YEAR
            ),
            whereCondition = whereCondition,
            selectionArgs = selectionArgs,
            orderBy = order(),
            orderAscending = isAscending(sorting),
            limit = pageSize,
            offset = (page) * pageSize
        )?.use {
            //Cache column indices.
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val artistIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val tracksColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
            val yearColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.FIRST_YEAR)
            while (it.moveToNext()) {
                val uri = "$URI$ALBUM_AUTH${it.getLong(idColumn)}"
                val coverUri = ContentUris.withAppendedId(
                    ARTWORK_URI, it.getLong(idColumn)
                )
                val artistId = "$URI$ARTIST_AUTH${it.getLong(artistIdColumn)}"
                albums.add(
                    Album(
                        id = uri,
                        title = it.getString(albumColumn),
                        cover = coverUri.toImageHolder(),
                        artists = listOf(Artist(artistId, it.getString(artistColumn))),
                        numberOfTracks = it.getInt(tracksColumn),
                        releaseDate = it.getString(yearColumn),
                        tracks = emptyList(),
                        publisher = null,
                        duration = null,
                        description = null,
                        subtitle = null
                    )
                )
            }
        }
        return albums
    }

    fun get(uri: Uri, trackResolver: TrackResolver): Album {
        val id = uri.lastPathSegment!!.toLong()
        val whereCondition = "${MediaStore.Audio.Albums._ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        val album = context.queryAlbums(whereCondition, selectionArgs, 0, 1, "").first()

        val tracks = trackResolver.getByAlbum(album, 0, 50, "date")
        val duration = tracks.sumOf { it.duration ?: 0 }

        return album.copy(
            tracks = tracks,
            duration = duration
        )
    }

    fun getShuffled(pageSize: Int): List<Album> {
        return getAll(0, 100, "").shuffled().take(pageSize)
    }
}