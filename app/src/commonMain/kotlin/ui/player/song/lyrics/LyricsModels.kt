package dev.brahmkshatriya.echo.app.ui.player.song.lyrics

import androidx.compose.runtime.Immutable

@Immutable
sealed interface Lyrics {
    @Immutable
    data class Simple(val text: String) : Lyrics

    @Immutable
    data class Line(val lines: List<LineLyric>) : Lyrics

    @Immutable
    data class Word(val lines: List<WordsLyric>) : Lyrics
}

@Immutable
enum class LyricsPosition {
    Start,
    End,
}

@Immutable
sealed interface TimedLyricsLine {
    val startMs: Long
    val endMs: Long
    val position: LyricsPosition
}

@Immutable
data class LineLyric(
    override val startMs: Long,
    override val endMs: Long,
    val text: String,
    val translations: List<Translation> = emptyList(),
    override val position: LyricsPosition = LyricsPosition.Start,
) : TimedLyricsLine

@Immutable
data class WordsLyric(
    override val startMs: Long,
    override val endMs: Long,
    val tokens: List<TimedToken>,
    val translations: List<Translation> = emptyList(),
    val backgroundVocals: List<TimedToken> = emptyList(),
    override val position: LyricsPosition = LyricsPosition.Start,
) : TimedLyricsLine

@Immutable
data class TimedToken(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val trailingSpace: String = "",
)

@Immutable
data class Translation(val language: String, val text: String)