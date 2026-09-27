package dev.brahmkshatriya.echo.app.ui.player.song.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import echo.client.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class LyricsSelectionItem(
    val title: String,
    val subtitle: String,
    val extension: String,
    val lyrics: Lyrics,
    val timingIndex: LyricsTimingIndex? = null,
)

@Composable
internal fun rememberLyricsSelections(): List<LyricsSelectionItem> {
    val selections by produceState(emptyList()) {
        value = withContext(Dispatchers.Default) { loadLyricsSelections() }
    }
    return selections
}

private suspend fun loadLyricsSelections(): List<LyricsSelectionItem> {
    val result = mutableListOf<LyricsSelectionItem>()
    for (relativePath in lyricsResourcePaths) {
        loadLyricsSelection(relativePath)?.let(result::add)
    }
    return result
}

private val lyricsResourcePaths = listOf(
    "Rap God - Eminem.ttml",
    "blackorwhite.ttml",
)

private suspend fun loadLyricsSelection(relativePath: String): LyricsSelectionItem? {
    val extension = relativePath.substringAfterLast('.', "").lowercase()
    if (extension !in supportedLyricsExtensions) return null

    val text = try {
        Res.readBytes("files/lyrics/$relativePath").decodeToString()
    } catch (_: Throwable) {
        return null
    }

    val lyrics = runCatching {
        when (extension) {
            "ttml" -> TtmlLyricsParser.parse(text)
            "lrc" -> LrcLyricsParser.parse(text)
            "json" -> LrcLibLyricsParser.parseJson(text)
            else -> return null
        }
    }.getOrNull() ?: return null

    val format = when (extension) {
        "ttml" -> "TTML"
        "lrc" -> "LRC"
        else -> "LRCLIB"
    }
    val syncType = when (lyrics) {
        is Lyrics.Word -> "Word synced lyrics"
        is Lyrics.Line -> "Line synced lyrics"
        is Lyrics.Simple -> "Plain lyrics"
    }
    val timingIndex = when (lyrics) {
        is Lyrics.Word -> LyricsTimingIndex.word(lyrics.lines)
        is Lyrics.Line -> LyricsTimingIndex.line(lyrics.lines)
        is Lyrics.Simple -> null
    }
    return LyricsSelectionItem(
        title = relativePath.substringAfterLast('/').substringBeforeLast('.').toDisplayTitle(),
        subtitle = "$format • $syncType",
        extension = format,
        lyrics = lyrics,
        timingIndex = timingIndex,
    )
}

private val supportedLyricsExtensions = setOf("ttml", "lrc", "json")

private fun String.toDisplayTitle(): String =
    replace('_', ' ')
        .replace('-', ' ')
        .trim()
        .split(Regex("\\s+"))
        .joinToString(" ") { word ->
            word.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }
