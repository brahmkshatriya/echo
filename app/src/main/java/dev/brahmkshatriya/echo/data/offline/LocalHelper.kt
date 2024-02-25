package dev.brahmkshatriya.echo.data.offline

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi

interface LocalHelper {

    companion object {

        const val URI = "local://"
        const val TRACK_AUTH = "track/"
        const val ALBUM_AUTH = "album/"
        const val ARTIST_AUTH = "artist/"

        val ARTWORK_URI = Uri.parse("content://media/external/audio/albumart")

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
                var order = "$orderBy $orderDirection"
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
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(orderBy)
            )
            val orderDirection = if (orderAscending)
                ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
            else
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, orderDirection)
            // Selection
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, whereCondition)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
        }
    }
}