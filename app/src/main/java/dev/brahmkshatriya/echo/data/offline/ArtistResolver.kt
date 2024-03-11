package dev.brahmkshatriya.echo.data.offline

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import dev.brahmkshatriya.echo.common.models.Artist

class ArtistResolver(val context: Context) {

    fun search(query: String, page: Int, pageSize: Int): List<Artist.WithCover> {
        val whereCondition =
            "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ? OR ${MediaStore.Audio.Media.ALBUM} LIKE ?"
        val selectionArgs = arrayOf("%$query%", "%$query%", "%$query%")
        return context.queryArtists(whereCondition, selectionArgs, page, pageSize)
            .sortedBy(query) {
                it.name
            }
    }

    fun getAll(page: Int, pageSize: Int): List<Artist.WithCover> {
        val whereCondition = ""
        val selectionArgs = arrayOf<String>()
        return context.queryArtists(whereCondition, selectionArgs, page, pageSize)
    }

    fun getShuffled(page: Int, pageSize: Int): List<Artist.WithCover> {
        return getAll(page, pageSize).shuffled()
    }

    private fun Context.queryArtists(
        whereCondition: String, selectionArgs: Array<String>, page: Int, pageSize: Int
    ): List<Artist.Full> {
        val artists = mutableListOf<Artist.Full>()
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
            offset = (page) * pageSize,
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
                    Artist.Full(
                        uri = uri,
                        name = it.getStringOrNull(artistColumn) ?: "THIS IS THE PROBLEM",
                        cover = null,
                        description = null,
                    )
                )
            }
        }
        return artists
    }

    fun get(uri: Uri) : Artist.Full {
        val id = uri.lastPathSegment!!.toLong()
        val whereCondition = "${MediaStore.Audio.Media.ARTIST_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        return context.queryArtists(whereCondition, selectionArgs, 0, 1).first()
    }
}