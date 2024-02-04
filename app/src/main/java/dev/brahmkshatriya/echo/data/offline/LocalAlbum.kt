package dev.brahmkshatriya.echo.data.offline

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dev.brahmkshatriya.echo.data.models.Album
import dev.brahmkshatriya.echo.data.models.Artist
import dev.brahmkshatriya.echo.data.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.data.offline.LocalHelper.Companion.ALBUM_AUTH
import dev.brahmkshatriya.echo.data.offline.LocalHelper.Companion.ARTIST_AUTH
import dev.brahmkshatriya.echo.data.offline.LocalHelper.Companion.URI
import dev.brahmkshatriya.echo.data.offline.LocalHelper.Companion.createCursor

interface LocalAlbum {

    companion object{

        fun search(context: Context, query: String, page: Int, pageSize: Int): List<Album.WithCover> {
            val whereCondition = "${MediaStore.Audio.Albums.ALBUM} LIKE ?"
            val selectionArgs = arrayOf("%$query%")
            return context.queryAlbums(whereCondition, selectionArgs, page, pageSize)
        }

        fun getAll(context: Context, page: Int, pageSize: Int): List<Album.WithCover> {
            val whereCondition = ""
            val selectionArgs = arrayOf<String>()
            return context.queryAlbums(whereCondition, selectionArgs, page, pageSize)
        }

        fun getByArtist(context: Context,artist: Artist.Small, page: Int, pageSize: Int): List<Album.WithCover> {
            val whereCondition = "${MediaStore.Audio.Media.ARTIST_ID} = ?"
            val selectionArgs = arrayOf(artist.uri.lastPathSegment!!)
            return context.queryAlbums(whereCondition, selectionArgs, page, pageSize)
        }

        private fun Context.queryAlbums(whereCondition: String, selectionArgs: Array<String>, page: Int, pageSize: Int): List<Album.WithCover> {
            val albums = mutableListOf<Album.WithCover>()
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
                orderBy = MediaStore.Audio.Albums.ALBUM,
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
    }
}