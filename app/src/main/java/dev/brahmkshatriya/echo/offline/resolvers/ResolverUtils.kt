package dev.brahmkshatriya.echo.offline.resolvers

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi


const val URI = "local://"
const val TRACK_AUTH = "track/"
const val ALBUM_AUTH = "album/"
const val ARTIST_AUTH = "artist/"

val ARTWORK_URI: Uri = Uri.parse("content://media/external/audio/albumart")

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

fun <E> List<E>.sortedBy(query: String, block: (E) -> String): List<E> {
    return sortedBy {
        val distance = wagnerFischer(block(it), query)

        val bonus = if (block(it).contains(query, true)) -20 else 0
        distance + bonus
    }
}

// taken from https://gist.github.com/jmarchesini/e330088e03daa394cf03ddedb8956fbe
fun wagnerFischer(s: String, t: String): Int {
    val m = s.length
    val n = t.length

    if (s == t) return 0
    if (s.isEmpty()) return n
    if (t.isEmpty()) return m

    val d = Array(m + 1) { IntArray(n + 1) { 0 } }

    (1..m).forEach { i ->
        d[i][0] = i
    }

    (1..n).forEach { j ->
        d[0][j] = j
    }

    (1..n).forEach { j ->
        (1..m).forEach { i ->
            val cost = if (s[i - 1] == t[j - 1]) 0 else 1
            val delCost = d[i - 1][j] + 1
            val addCost = d[i][j - 1] + 1
            val subCost = d[i - 1][j - 1] + cost

            d[i][j] = minOf(delCost, addCost, subCost)
        }
    }

    return d[m][n]
}
