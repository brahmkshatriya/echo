package dev.brahmkshatriya.echo.data.offline

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.data.offline.LocalHelper.Companion.ALBUM_AUTH
import dev.brahmkshatriya.echo.data.offline.LocalHelper.Companion.ARTIST_AUTH
import dev.brahmkshatriya.echo.data.offline.LocalHelper.Companion.ARTWORK_URI
import dev.brahmkshatriya.echo.data.offline.LocalHelper.Companion.URI
import dev.brahmkshatriya.echo.data.offline.LocalHelper.Companion.createCursor

interface LocalAlbum {

    companion object {

        fun search(
            context: Context,
            query: String,
            page: Int,
            pageSize: Int
        ): List<Album.WithCover> {
            val whereCondition =
                "${MediaStore.Audio.Media.ARTIST} LIKE ? OR ${MediaStore.Audio.Media.ALBUM} LIKE ?"
            val selectionArgs = arrayOf("%$query%", "%$query%")
            return context.queryAlbums(whereCondition, selectionArgs, page, pageSize)
                .sortedBy(query) {
                    it.title
                }
        }

        fun getAll(context: Context, page: Int, pageSize: Int): List<Album.WithCover> {
            val whereCondition = ""
            val selectionArgs = arrayOf<String>()
            return context.queryAlbums(whereCondition, selectionArgs, page, pageSize)
        }

        fun getByArtist(
            context: Context,
            artist: Artist.Small,
            page: Int,
            pageSize: Int
        ): List<Album.WithCover> {
            val whereCondition = "${MediaStore.Audio.Media.ARTIST} LIKE ?"
            val selectionArgs = arrayOf("%${artist.name}%")
            return context.queryAlbums(whereCondition, selectionArgs, page, pageSize)
        }

        private fun Context.queryAlbums(
            whereCondition: String,
            selectionArgs: Array<String>,
            page: Int,
            pageSize: Int
        ): MutableList<Album.Full> {
            val albums = mutableListOf<Album.Full>()
            createCursor(
                contentResolver = contentResolver,
                collection = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection = arrayOf(
                    MediaStore.Audio.Albums._ID,
                    MediaStore.Audio.Albums.ALBUM,
                    MediaStore.Audio.Albums.ARTIST,
                    MediaStore.Audio.Albums.NUMBER_OF_SONGS,
                    MediaStore.Audio.Albums.FIRST_YEAR
                ),
                whereCondition = whereCondition,
                selectionArgs = selectionArgs,
                orderBy = MediaStore.Audio.Albums.ALBUM,
                orderAscending = true,
                limit = pageSize,
                offset = (page - 1) * pageSize
            )?.use {
                //Cache column indices.
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
                val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
                val tracksColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
                val yearColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.FIRST_YEAR)
                while (it.moveToNext()) {
                    val uri = Uri.parse("$URI$ALBUM_AUTH${it.getLong(idColumn)}")
                    val coverUri = ContentUris.withAppendedId(
                        ARTWORK_URI,
                        it.getLong(idColumn)
                    )
                    val artistUri = Uri.parse("$URI$ARTIST_AUTH${it.getLong(idColumn)}")
                    albums.add(
                        Album.Full(
                            uri = uri,
                            title = it.getString(albumColumn),
                            cover = coverUri.toImageHolder(),
                            artist = Artist.Small(artistUri, it.getString(artistColumn)),
                            numberOfTracks = it.getInt(tracksColumn),
                            releaseDate = it.getString(yearColumn),
                            tracks =  emptyList(),
                            publisher = null,
                            duration = null,
                            description = null
                        )
                    )
                }
            }
            return albums
        }

        fun get(context: Context, uri: Uri): Album.Full {
            val id = uri.lastPathSegment!!.toLong()
            val whereCondition = "${MediaStore.Audio.Albums._ID} = ?"
            val selectionArgs = arrayOf(id.toString())
            val album = context.queryAlbums(whereCondition, selectionArgs, 0, 1).first()
            val tracks = LocalTrack.getByAlbum(context, album, 0, 50)
            val duration = tracks.sumOf { it.duration ?: 0 }
            return album.copy(
                tracks = LocalTrack.getByAlbum(context, album, 0, 50),
                duration = duration
            )
        }

        fun getShuffled(context: Context, page: Int, pageSize: Int): List<Album.WithCover> {
            return getAll(context, page, pageSize).shuffled()
        }
    }
}