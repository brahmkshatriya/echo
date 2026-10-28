package dev.brahmkshatriya.echo.app.ui.player.song.lyrics

/** A complete written word, which may span several individually timed syllables. */
internal data class MotionWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

/**
 * TTML spans are not guaranteed to be whole words: a word can be split into
 * several syllables or a span can contain multiple whitespace-separated words.
 * Whitespace (including [TimedToken.trailingSpace]) ends a word; unseparated
 * spans, clitics and hyphenated fragments form one displayed word. Terminal
 * punctuation attaches to its word but separates it from the following word.
 */
internal fun groupMotionWords(lines: List<WordsLyric>): List<MotionWord> = buildList {
    for ((_, _, tokens) in lines) {
        val text = StringBuilder()
        var startMs = 0L
        var endMs = 0L

        fun emit() {
            val complete = text.toString().trim()
            if (complete.isNotEmpty()) {
                add(MotionWord(complete, startMs, endMs.coerceAtLeast(startMs + 1L)))
            }
            text.clear()
        }

        fun append(fragment: String, fromMs: Long, toMs: Long) {
            if (fragment.isEmpty()) return
            // A new letter after sentence punctuation starts a new word even
            // when the source omitted the whitespace between timed spans.
            if (text.isNotEmpty() && fragment.first().isLetterOrDigit() &&
                text.last() in ".,;:!?…。！？") emit()
            if (text.isEmpty()) {
                startMs = fromMs
                endMs = toMs
            } else {
                endMs = maxOf(endMs, toMs)
            }
            text.append(fragment)
        }

        for ((source, startMs1, endMs1, trailingSpace) in tokens) {
            val duration = (endMs1 - startMs1).coerceAtLeast(0L)
            var cursor = 0
            while (cursor < source.length) {
                if (source[cursor].isWhitespace()) {
                    emit()
                    cursor++
                    continue
                }
                val first = cursor
                while (cursor < source.length && !source[cursor].isWhitespace()) cursor++
                val from = startMs1 + duration * first / source.length
                val to = startMs1 + duration * cursor / source.length
                append(source.substring(first, cursor), from, to)
            }

            // Trailing content can be a space (word boundary), a connector
            // like a hyphen/apostrophe, or punctuation belonging to this word.
            for (character in trailingSpace) {
                if (character.isWhitespace()) emit()
                else append(character.toString(), endMs1, endMs1)
            }
        }
        emit() // A line break also separates words, even with no trailing space.
    }
}.sortedBy(MotionWord::startMs)

/** Binary lookup so seeking and frame-by-frame animation remain inexpensive. */
internal class MotionWordIndex(val words: List<MotionWord>) {
    fun at(positionMs: Long): MotionWord? {
        var low = 0
        var high = words.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (words[mid].startMs <= positionMs) low = mid + 1 else high = mid
        }
        val word = words.getOrNull(low - 1) ?: return null
        return word.takeIf { positionMs < it.endMs }
    }
}
