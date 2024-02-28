package dev.brahmkshatriya.echo.data.offline

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.data.offline.LocalHelper.Companion.ARTIST_AUTH
import dev.brahmkshatriya.echo.data.offline.LocalHelper.Companion.URI
import dev.brahmkshatriya.echo.data.offline.LocalHelper.Companion.createCursor

interface LocalArtist {

    companion object {
        fun search(
            context: Context, query: String, page: Int, pageSize: Int
        ): List<Artist.WithCover> {
            val whereCondition =
                "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ? OR ${MediaStore.Audio.Media.ALBUM} LIKE ?"
            val selectionArgs = arrayOf("%$query%", "%$query%", "%$query%")
            return context.queryArtists(whereCondition, selectionArgs, page, pageSize)
                .sortedBy(query) {
                    it.name
                }
        }

        fun getAll(context: Context, page: Int, pageSize: Int): List<Artist.WithCover> {
            val whereCondition = ""
            val selectionArgs = arrayOf<String>()
            return context.queryArtists(whereCondition, selectionArgs, page, pageSize)
        }

        private fun Context.queryArtists(
            whereCondition: String, selectionArgs: Array<String>, page: Int, pageSize: Int
        ): List<Artist.WithCover> {
            val artists = mutableListOf<Artist.WithCover>()
            createCursor(
                contentResolver = contentResolver,
                collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection = arrayOf(
                    MediaStore.Audio.Media.ARTIST_ID,
                    MediaStore.Audio.Media.ARTIST,
                ),
                whereCondition = whereCondition,
                selectionArgs = selectionArgs,
                orderBy = MediaStore.Audio.Media.ARTIST,
                orderAscending = true,
                limit = pageSize,
                offset = (page - 1) * pageSize,
            )?.use {
                //Cache column indices.
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

                val ids = mutableListOf<Long>()
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    if (ids.contains(id)) continue
                    ids.add(id)
                    val uri = Uri.parse("$URI$ARTIST_AUTH$id")
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