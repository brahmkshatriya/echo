package dev.brahmkshatriya.echo.app.ui.player

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
    val trailingSpace: String = ""
)

@Immutable
data class Translation(val language: String, val text: String)

val lineLyricsExample = Lyrics.Line(
    lines = listOf(
        LineLyric(
            startMs = 0L,
            endMs = 3_000L,
            text = "Line timed lyrics display one full line at a time",
            translations = listOf(
                Translation(language = "zh-CN", text = "逐行歌词一次显示整行")
            )
        ),
        LineLyric(
            startMs = 3_800L,
            endMs = 7_400L,
            text = "Translations belong to each timed line",
            translations = listOf(
                Translation(language = "zh-CN", text = "翻译跟随每一行歌词")
            )
        ),
        LineLyric(
            startMs = 10_200L,
            endMs = 13_600L,
            text = "Long gaps still show the animated waiting dots",
            translations = listOf(
                Translation(language = "zh-CN", text = "较长间隔仍会显示等待动画")
            ),
            position = LyricsPosition.End,
        ),
        LineLyric(
            startMs = 14_000L,
            endMs = 17_400L,
            text = "No word timing is needed for this lyrics type",
            translations = listOf(
                Translation(language = "zh-CN", text = "这种歌词类型不需要逐词时间轴")
            ),
            position = LyricsPosition.End,
        ),
        LineLyric(
            startMs = 18_000L,
            endMs = 22_000L,
            text = "Use Lyrics.Word when token-level sync is available",
            translations = listOf(
                Translation(language = "zh-CN", text = "有逐词同步时使用 Lyrics.Word")
            )
        )
    )
)

/*
 * The original files contained large manually supplied word-timing datasets.
 * Their MFT records were damaged and the complete datasets were never printed
 * in the recovery logs. These compact entries preserve the recovered model and
 * exercise token timing, spacing, translations, gaps, and background vocals.
 */
val selfLoveLyrics = Lyrics.Word(
    lines = listOf(
        WordsLyric(
            startMs = 0L,
            endMs = 2_400L,
            tokens = listOf(
                TimedToken("Timed", 0L, 700L, " "),
                TimedToken("lyrics", 700L, 1_500L, " "),
                TimedToken("example", 1_500L, 2_400L)
            )
        ),
        WordsLyric(
            startMs = 3_200L,
            endMs = 6_000L,
            tokens = listOf(
                TimedToken("Words", 3_200L, 4_000L, " "),
                TimedToken("pan", 4_000L, 4_700L, " "),
                TimedToken("as", 4_700L, 5_100L, " "),
                TimedToken("they", 5_100L, 5_500L, " "),
                TimedToken("play", 5_500L, 6_000L)
            ),
            translations = listOf(
                Translation("example", "Translation example")
            ),
            position = LyricsPosition.End,
        )
    )
)

val rapGodLyrics: Lyrics = Lyrics.Word(
    lines = listOf(
        WordsLyric(
            startMs = 0L,
            endMs = 1_800L,
            tokens = listOf(
                TimedToken("Fast", 0L, 400L, " "),
                TimedToken("timing", 400L, 1_000L, " "),
                TimedToken("sample", 1_000L, 1_800L)
            ),
            backgroundVocals = listOf(
                TimedToken("Background", 300L, 1_500L)
            )
        ),
        WordsLyric(
            startMs = 2_600L,
            endMs = 4_800L,
            tokens = listOf(
                TimedToken("Another", 2_600L, 3_300L, " "),
                TimedToken("timed", 3_300L, 4_000L, " "),
                TimedToken("line", 4_000L, 4_800L)
            )
        )
    )
)
