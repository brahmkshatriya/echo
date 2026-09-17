package dev.brahmkshatriya.echo.app.ui.player.song.lyrics

import dev.brahmkshatriya.echo.app.platform.LyricsBloom
import dev.brahmkshatriya.echo.app.ui.player.song.LyricsWaitingDotsPostRollMs
import dev.brahmkshatriya.echo.app.ui.player.song.LyricsWaitingDotsPreRollMs
import dev.brahmkshatriya.echo.app.ui.player.song.LyricsWaitingGapMs

internal data class LyricsWaitingGap(
    val afterLineIndex: Int,
    val startMs: Long,
    val endMs: Long,
) {
    fun contains(positionMs: Long): Boolean = positionMs in (startMs + 1)..<endMs

    fun isVisibleInFullLyrics(positionMs: Long): Boolean {
        val visibleStartMs = (startMs - LyricsWaitingDotsPreRollMs).coerceAtLeast(0L)
        val visibleEndMs = endMs + LyricsWaitingDotsPostRollMs
        return positionMs in visibleStartMs..visibleEndMs
    }
}

private fun upperBound(values: LongArray, value: Long): Int {
    var low = 0
    var high = values.size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (values[mid] <= value) low = mid + 1 else high = mid
    }
    return low
}

private fun lowerBound(values: LongArray, value: Long): Int {
    var low = 0
    var high = values.size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (values[mid] < value) low = mid + 1 else high = mid
    }
    return low
}

internal sealed interface FullLyricsLineTiming {
    val line: TimedLyricsLine
    val text: String
}

internal class WordLineTiming(override val line: WordsLyric) : FullLyricsLineTiming {
    override val text = buildString {
        line.tokens.forEach { token ->
            append(token.text)
            append(token.trailingSpace)
        }
    }
    val tokenOffsets = IntArray(line.tokens.size)
    private val tokenStarts = LongArray(line.tokens.size)
    private val tokenLengths = IntArray(line.tokens.size)
    val globalGlyphPositions = FloatArray(text.length) { it.toFloat() }

    init {
        var offset = 0
        line.tokens.forEachIndexed { index, token ->
            tokenOffsets[index] = offset
            tokenStarts[index] = token.startMs
            tokenLengths[index] = (token.text.length + token.trailingSpace.length).coerceAtLeast(1)
            offset += token.text.length + token.trailingSpace.length
        }
    }

    fun tokenIndexAt(positionMs: Long): Int = upperBound(tokenStarts, positionMs) - 1

    fun bloomAt(positionMs: Long): LyricsBloom? {
        if (line.tokens.isEmpty()) return null
        val tokenIndex = tokenIndexAt(positionMs)
        val token = line.tokens.getOrNull(tokenIndex) ?: return null
        if (positionMs < token.startMs || positionMs > token.endMs) return null

        val startPosition = tokenOffsets[tokenIndex]
        val endPositionExclusive = (startPosition + token.text.length)
            .coerceAtMost(text.length)
        if (endPositionExclusive <= startPosition) return null

        fun smoothRamp(value: Float): Float {
            val t = value.coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }

        val elapsedMs = (positionMs - token.startMs).coerceAtLeast(0L).toFloat()
        val remainingMs = (token.endMs - positionMs).coerceAtLeast(0L).toFloat()
        val attack = smoothRamp(elapsedMs / 650f)
        val release = smoothRamp(remainingMs / 220f)
        return LyricsBloom(startPosition, endPositionExclusive, attack * release)
    }

    fun revealPositionAt(positionMs: Long): Float {
        if (line.tokens.isEmpty()) return 0f
        val tokenIndex = tokenIndexAt(positionMs)
        val token = line.tokens.getOrNull(tokenIndex) ?: return 0f
        val tokenProgress = if (token.endMs > token.startMs) {
            ((positionMs - token.startMs).toFloat() /
                    (token.endMs - token.startMs).toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }
        val start = tokenOffsets[tokenIndex].toFloat()
        val length = tokenLengths[tokenIndex].toFloat()
        return (start + length * tokenProgress).coerceIn(0f, text.length.toFloat())
    }
}

private data class PeakTimelineSegment(
    val startMs: Long,
    val endMs: Long,
    val startPosition: Float,
    val endPosition: Float,
)

private class LyricsPeakTimeline private constructor(
    private val segments: List<PeakTimelineSegment>,
    private val segmentStarts: LongArray,
    private val averageCharacterDurationMs: Float,
    private val lastTimedEndMs: Long,
    private val lastPosition: Float,
) {
    companion object {
        fun build(lines: List<WordLineTiming>): LyricsPeakTimeline {
            data class TokenRun(
                val lineTiming: WordLineTiming,
                val tokenIndex: Int,
                val token: TimedToken,
            )

            val runs = lines.flatMap { lineTiming ->
                lineTiming.line.tokens.mapIndexed { tokenIndex, token ->
                    TokenRun(lineTiming, tokenIndex, token)
                }
            }.sortedBy { it.token.startMs }

            var totalTimedDurationMs = 0L
            var totalCharacters = 0
            runs.forEach { run ->
                val characterCount = run.token.text.length + run.token.trailingSpace.length
                if (characterCount > 0 && run.token.endMs > run.token.startMs) {
                    totalTimedDurationMs += run.token.endMs - run.token.startMs
                    totalCharacters += characterCount
                }
            }
            val averageCharacterDurationMs = if (totalCharacters > 0 && totalTimedDurationMs > 0L) {
                totalTimedDurationMs.toFloat() / totalCharacters.toFloat()
            } else {
                120f
            }

            val segments = mutableListOf<PeakTimelineSegment>()
            var cursorMs = 0L
            var cursorPosition = 0f
            runs.forEach { run ->
                val token = run.token
                val characterCount = token.text.length + token.trailingSpace.length
                if (characterCount <= 0) return@forEach

                if (token.startMs > cursorMs) {
                    val gapEndPosition = cursorPosition +
                            (token.startMs - cursorMs).toFloat() / averageCharacterDurationMs
                    segments += PeakTimelineSegment(
                        startMs = cursorMs,
                        endMs = token.startMs,
                        startPosition = cursorPosition,
                        endPosition = gapEndPosition,
                    )
                    cursorPosition = gapEndPosition
                    cursorMs = token.startMs
                }

                val tokenOffset = run.lineTiming.tokenOffsets[run.tokenIndex]
                repeat(characterCount) { characterIndex ->
                    val textIndex = tokenOffset + characterIndex
                    if (textIndex in run.lineTiming.globalGlyphPositions.indices) {
                        run.lineTiming.globalGlyphPositions[textIndex] =
                            cursorPosition + characterIndex
                    }
                }

                val tokenEndPosition = cursorPosition + characterCount
                val effectiveStartMs = maxOf(cursorMs, token.startMs)
                if (token.endMs > effectiveStartMs) {
                    segments += PeakTimelineSegment(
                        startMs = effectiveStartMs,
                        endMs = token.endMs,
                        startPosition = cursorPosition,
                        endPosition = tokenEndPosition,
                    )
                }
                cursorPosition = tokenEndPosition
                cursorMs = maxOf(cursorMs, token.endMs)
            }

            return LyricsPeakTimeline(
                segments = segments,
                segmentStarts = LongArray(segments.size) { segments[it].startMs },
                averageCharacterDurationMs = averageCharacterDurationMs,
                lastTimedEndMs = cursorMs,
                lastPosition = cursorPosition,
            )
        }
    }

    fun positionAt(positionMs: Long, songDurationMs: Long): Float {
        val timeMs = positionMs.coerceAtLeast(0L)
        val segmentIndex = upperBound(segmentStarts, timeMs) - 1
        val segment = segments.getOrNull(segmentIndex)
        if (segment != null && timeMs <= segment.endMs) {
            val durationMs = (segment.endMs - segment.startMs).coerceAtLeast(1L)
            val progress = ((timeMs - segment.startMs).toFloat() / durationMs.toFloat())
                .coerceIn(0f, 1f)
            return segment.startPosition +
                    (segment.endPosition - segment.startPosition) * progress
        }

        val tailEndMs = songDurationMs.coerceAtLeast(lastTimedEndMs)
        val tailTimeMs = timeMs.coerceIn(lastTimedEndMs, tailEndMs)
        return lastPosition +
                (tailTimeMs - lastTimedEndMs).toFloat() / averageCharacterDurationMs
    }
}

internal class LineTiming(override val line: LineLyric) : FullLyricsLineTiming {
    override val text: String = line.text
}

internal class LyricsTimingIndex private constructor(
    val lines: List<FullLyricsLineTiming>,
    private val peakTimeline: LyricsPeakTimeline? = null,
) {
    private val lineStarts = LongArray(lines.size) { lines[it].line.startMs }
    private val lineEnds = LongArray(lines.size) { lines[it].line.endMs }

    companion object {
        fun word(lines: List<WordsLyric>): LyricsTimingIndex {
            val timings = lines.map(::WordLineTiming)
            return LyricsTimingIndex(
                lines = timings,
                peakTimeline = LyricsPeakTimeline.build(timings),
            )
        }

        fun line(lines: List<LineLyric>) = LyricsTimingIndex(lines.map(::LineTiming))
    }

    fun waitingGapBeforeFirst(): LyricsWaitingGap? {
        val firstLine = lines.firstOrNull()?.line ?: return null
        if (firstLine.startMs < LyricsWaitingGapMs) return null
        return LyricsWaitingGap(
            afterLineIndex = -1,
            startMs = 0L,
            endMs = firstLine.startMs,
        )
    }

    fun waitingGapAfter(lineIndex: Int): LyricsWaitingGap? {
        val line = lines.getOrNull(lineIndex)?.line ?: return null
        val nextLine = lines.getOrNull(lineIndex + 1)?.line ?: return null
        val gapMs = nextLine.startMs - line.endMs
        if (gapMs < LyricsWaitingGapMs) return null
        return LyricsWaitingGap(
            afterLineIndex = lineIndex,
            startMs = line.endMs,
            endMs = nextLine.startMs,
        )
    }

    fun waitingGapAt(positionMs: Long): LyricsWaitingGap? {
        val previousIndex = lineIndexAtOrBefore(positionMs)
        val gap = if (previousIndex < 0) waitingGapBeforeFirst() else waitingGapAfter(previousIndex)
        return gap?.takeIf { it.contains(positionMs) }
    }

    fun peakPositionAt(positionMs: Long, songDurationMs: Long): Float? =
        peakTimeline?.positionAt(positionMs, songDurationMs)

    fun lineIndexAtOrBefore(positionMs: Long): Int = upperBound(lineStarts, positionMs) - 1

    fun currentLineIndex(positionMs: Long): Int = lineIndexAtOrBefore(positionMs).coerceAtLeast(0)

    fun activeLineIndex(positionMs: Long): Int {
        val index = lineIndexAtOrBefore(positionMs)
        val line = lines.getOrNull(index)?.line ?: return -1
        return if (positionMs in line.startMs..line.endMs) index else -1
    }

    fun latestCompletedLineIndex(positionMs: Long): Int = lowerBound(lineEnds, positionMs) - 1

    fun scrollTargetIndex(positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        val lookAheadMs = if (lines.firstOrNull() is LineTiming) 250L else 0L
        val anticipatedPosition = positionMs + lookAheadMs
        val previousIndex = lineIndexAtOrBefore(anticipatedPosition)
        if (previousIndex < 0) return 0

        val previousLine = lines[previousIndex].line
        if (anticipatedPosition <= previousLine.endMs) return previousIndex

        return (previousIndex + 1).coerceAtMost(lines.lastIndex)
    }
}
