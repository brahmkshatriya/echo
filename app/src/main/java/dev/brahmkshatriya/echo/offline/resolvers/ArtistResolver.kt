package dev.brahmkshatriya.echo.offline.resolvers

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder

class ArtistResolver(val context: Context) {

    private fun order() = MediaStore.Audio.Media.ARTIST
    private fun isAscending(sorting: String) = when (sorting) {
        "a_to_z" -> true
        "z_to_a" -> false
        else -> true
    }

    fun search(
        list: List<Artist>,
        query: String,
        page: Int,
        pageSize: Int,
        sorting: String
    ): List<Artist> {
        val whereCondition =
            "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ? OR ${MediaStore.Audio.Media.ALBUM} LIKE ?"
        val selectionArgs = arrayOf("%$query%", "%$query%", "%$query%")
        return context.queryArtists(list, whereCondition, selectionArgs, page, pageSize, sorting)
            .sortedBy(query) {
                it.name
            }
    }

    fun getAll(list: List<Artist>, page: Int, pageSize: Int, sorting: String): List<Artist> {
        val whereCondition = ""
        val selectionArgs = arrayOf<String>()
        return context.queryArtists(list, whereCondition, selectionArgs, page, pageSize, sorting)
    }

    fun getShuffled(pageSize: Int): List<Artist> {
        return getAll(listOf(), 0, 100, "").shuffled().take(pageSize)
    }

    private fun Context.queryArtists(
        oldList: List<Artist>,
        whereCondition: String,
        selectionArgs: Array<String>,
        page: Int,
        pageSize: Int,
        sorting: String
    ): List<Artist> {
        val artists = mutableListOf<Artist>()
        createCursor(
            contentResolver = contentResolver,
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection = arrayOf(
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID,
            ),
            whereCondition = whereCondition,
            selectionArgs = selectionArgs,
            orderBy = order(),
            orderAscending = isAscending(sorting),
            limit = pageSize,
            offset = page * pageSize
        )?.use {
            val artistIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            val ids = oldList.map { artist ->
                artist.id.toUri().lastPathSegment!!.toLong()
            }.toMutableList()
            while (it.moveToNext()) {
                val id = it.getLong(artistIdColumn)
                val albumId = it.getLong(albumIdColumn)
                val coverUri = ContentUris.withAppendedId(
                    ARTWORK_URI, albumId
                )
                if (ids.contains(id)) continue
                ids.add(id)
                val uri = "$URI$ARTIST_AUTH$id"
                artists.add(
                    Artist(
                        id = uri,
                        name = it.getStringOrNull(artistColumn) ?: "THIS IS THE PROBLEM",
                        cover = coverUri.toImageHolder(),
                        description = null,
                    )
                )
            }
        }
        return artists
    }

    fun get(uri: Uri): Artist {
        val id = uri.lastPathSegment!!.toLong()
        val whereCondition = "${MediaStore.Audio.Media.ARTIST_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        return context.queryArtists(listOf(), whereCondition, selectionArgs, 0, 1, "").first()
    }
}