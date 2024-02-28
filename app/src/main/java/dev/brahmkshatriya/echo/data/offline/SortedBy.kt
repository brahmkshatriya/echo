package dev.brahmkshatriya.echo.data.offline

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
