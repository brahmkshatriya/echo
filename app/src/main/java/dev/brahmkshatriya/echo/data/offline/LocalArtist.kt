package dev.brahmkshatriya.echo.data.offline

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dev.brahmkshatriya.echo.data.models.Artist
import dev.brahmkshatriya.echo.data.offline.LocalHelper.Companion.ARTIST_AUTH
import dev.brahmkshatriya.echo.data.offline.LocalHelper.Companion.URI
import dev.brahmkshatriya.echo.data.offline.LocalHelper.Companion.createCursor

interface LocalArtist {

    companion object{
        fun search(context: Context, query: String, page: Int, pageSize: Int): List<Artist.WithCover> {
            val whereCondition = "${MediaStore.Audio.Artists.ARTIST} LIKE ?"
            val selectionArgs = arrayOf("%$query%")
            return context.queryArtists(whereCondition, selectionArgs, page, pageSize)
        }

        fun getAll(context: Context,page: Int, pageSize: Int): List<Artist.WithCover> {
            val whereCondition = ""
            val selectionArgs = arrayOf<String>()
            return context.queryArtists(whereCondition, selectionArgs, page, pageSize)
        }

        private fun Context.queryArtists(whereCondition: String, selectionArgs: Array<String>, page: Int, pageSize: Int): List<Artist.WithCover> {
            val artists = mutableListOf<Artist.WithCover>()
            createCursor(
                contentResolver = contentResolver,
                collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection = arrayOf(
                    MediaStore.Audio.Artists._ID,
                    MediaStore.Audio.Artists.ARTIST,
                ),
                whereCondition = whereCondition,
                selectionArgs = selectionArgs,
                orderBy = MediaStore.Audio.Artists.ARTIST,
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
    }
}