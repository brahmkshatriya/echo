package dev.brahmkshatriya.echo.app.ui.player.song.lyrics

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

internal object TtmlLyricsParser {
    private val paragraphRegex = Regex(
        pattern = "(?is)<p\\b([^>]*)>(.*?)</p>",
    )
    private val spanRegex = Regex(
        pattern = "(?is)<span\\b([^>]*)>(.*?)</span>",
    )
    private val spanTagRegex = Regex(
        pattern = "(?is)<(/?)span\\b([^>]*)>",
    )
    private val attributeRegex = Regex(
        pattern = "(?s)([A-Za-z_][\\w:.-]*)\\s*=\\s*([\"'])(.*?)\\2",
    )
    private val tagRegex = Regex("<[^>]+>")

    fun parse(ttml: String): Lyrics {
        val wordLines = mutableListOf<WordsLyric>()
        val lineLines = mutableListOf<LineLyric>()

        paragraphRegex.findAll(ttml).forEach { paragraph ->
            val attrs = parseAttributes(paragraph.groupValues[1])
            val body = paragraph.groupValues[2]
            val startMs = parseTimeMs(attrs["begin"]) ?: return@forEach
            val explicitEndMs = parseTimeMs(attrs["end"])
                ?: parseTimeMs(attrs["dur"])?.let(startMs::plus)

            val parsedSpans = parseSpans(body)
            val translations = parsedSpans.translations
            val tokens = parsedSpans.tokens
            val backgroundVocals = parsedSpans.backgroundVocals

            val paragraphEndMs = explicitEndMs
                ?: tokens.maxOfOrNull(TimedToken::endMs)
                ?: backgroundVocals.maxOfOrNull(TimedToken::endMs)
                ?: startMs
            val position = parsePosition(attrs)

            if (tokens.isNotEmpty()) {
                wordLines += WordsLyric(
                    startMs = startMs,
                    endMs = paragraphEndMs,
                    tokens = tokens,
                    translations = translations,
                    backgroundVocals = backgroundVocals,
                    position = position,
                )
            } else {
                val textWithoutTranslations = removeTranslationSpans(body)
                val text = plainText(textWithoutTranslations)
                if (text.isNotEmpty()) {
                    lineLines += LineLyric(
                        startMs = startMs,
                        endMs = paragraphEndMs,
                        text = text,
                        translations = translations,
                        position = position,
                    )
                }
            }
        }

        return when {
            wordLines.isNotEmpty() -> Lyrics.Word(wordLines.sortedBy(WordsLyric::startMs))
            lineLines.isNotEmpty() -> Lyrics.Line(lineLines.sortedBy(LineLyric::startMs))
            else -> Lyrics.Simple(plainText(ttml))
        }
    }

    private data class ParsedSpans(
        val tokens: List<TimedToken>,
        val translations: List<Translation>,
        val backgroundVocals: List<TimedToken>,
    )

    private data class SpanContext(
        val attrs: Map<String, String>,
        val background: Boolean,
        val translation: Boolean,
        val text: StringBuilder = StringBuilder(),
        var hasTimedDescendant: Boolean = false,
    )

    private data class EmittedToken(
        val background: Boolean,
        val index: Int,
    )

    private fun parseSpans(body: String): ParsedSpans {
        val tokens = mutableListOf<TimedToken>()
        val translations = mutableListOf<Translation>()
        val backgroundVocals = mutableListOf<TimedToken>()
        val stack = mutableListOf<SpanContext>()
        var cursor = 0
        var pendingWhitespace = false
        var lastEmittedToken: EmittedToken? = null

        fun appendText(rawText: String) {
            if (rawText.isEmpty()) return
            stack.forEach { it.text.append(rawText) }
            if (lastEmittedToken != null && rawText.any(Char::isWhitespace)) {
                pendingWhitespace = true
            }
        }

        fun addToken(token: TimedToken, background: Boolean) {
            val previous = lastEmittedToken
            if (pendingWhitespace && previous != null && previous.background == background) {
                val list = if (background) backgroundVocals else tokens
                val oldToken = list[previous.index]
                if (oldToken.trailingSpace.isEmpty()) {
                    list[previous.index] = oldToken.copy(trailingSpace = " ")
                }
            }
            pendingWhitespace = false

            val list = if (background) backgroundVocals else tokens
            list += token
            lastEmittedToken = EmittedToken(background = background, index = list.lastIndex)
        }

        spanTagRegex.findAll(body).forEach { tag ->
            appendText(body.substring(cursor, tag.range.first))
            cursor = tag.range.last + 1

            val closing = tag.groupValues[1].isNotEmpty()
            if (!closing) {
                val attrs = parseAttributes(tag.groupValues[2])
                val role = attrs.role()
                val parent = stack.lastOrNull()
                stack += SpanContext(
                    attrs = attrs,
                    background = parent?.background == true || role.isBackgroundRole(),
                    translation = parent?.translation == true || role.isTranslationRole(),
                )
                return@forEach
            }

            val context = stack.removeLastOrNull() ?: return@forEach
            val text = plainText(context.text.toString())
            val tokenStartMs = parseTimeMs(context.attrs["begin"])
            val tokenEndMs = parseTimeMs(context.attrs["end"])
                ?: parseTimeMs(context.attrs["dur"])?.let { duration ->
                    tokenStartMs?.plus(duration)
                }
            val isTimed = tokenStartMs != null && tokenEndMs != null

            if (context.translation) {
                if (text.isNotEmpty()) {
                    val language = context.attrs["xml:lang"] ?: context.attrs["lang"] ?: "und"
                    translations += Translation(language, text)
                }
            } else if (isTimed && !context.hasTimedDescendant && text.isNotEmpty()) {
                addToken(
                    token = TimedToken(
                        text = text,
                        startMs = tokenStartMs,
                        endMs = tokenEndMs,
                    ),
                    background = context.background,
                )
            }

            if (isTimed || context.hasTimedDescendant) {
                stack.lastOrNull()?.hasTimedDescendant = true
            }
        }
        appendText(body.substring(cursor))

        return ParsedSpans(
            tokens = tokens,
            translations = translations,
            backgroundVocals = backgroundVocals,
        )
    }

    private fun Map<String, String>.role(): String = entries
        .firstOrNull { it.key.endsWith(":role") || it.key == "role" }
        ?.value
        ?.lowercase()
        .orEmpty()

    private fun String.isTranslationRole(): Boolean =
        "translation" in this || "translated" in this

    private fun String.isBackgroundRole(): Boolean =
        "background" in this || "x-bg" in this || "x-background" in this

    private fun parseAttributes(source: String): Map<String, String> = buildMap {
        attributeRegex.findAll(source).forEach { match ->
            put(match.groupValues[1], decodeXml(match.groupValues[3]))
        }
    }

    private fun parsePosition(attrs: Map<String, String>): LyricsPosition {
        val value = attrs.entries
            .firstOrNull { it.key.endsWith(":textAlign") || it.key == "textAlign" }
            ?.value
            ?.lowercase()
        return if (value == "end" || value == "right") LyricsPosition.End else LyricsPosition.Start
    }

    private fun removeTranslationSpans(body: String): String = spanRegex.replace(body) { match ->
        val attrs = parseAttributes(match.groupValues[1])
        val role = attrs.entries
            .firstOrNull { it.key.endsWith(":role") || it.key == "role" }
            ?.value
            ?.lowercase()
            .orEmpty()
        if ("translation" in role || "translated" in role) "" else match.value
    }

    private fun plainText(source: String): String = decodeXml(
        source
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(tagRegex, "")
    ).replace(Regex("[ \\t\\r\\f\\u000B]+"), " ")
        .replace(Regex(" *\\n *"), "\n")
        .trim()

    private fun decodeXml(value: String): String = value
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")

    internal fun parseTimeMs(value: String?): Long? {
        val raw = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (raw.endsWith("ms", ignoreCase = true)) {
            return raw.dropLast(2).toDoubleOrNull()?.toLong()
        }
        if (raw.endsWith("s", ignoreCase = true)) {
            return raw.dropLast(1).toDoubleOrNull()?.times(1_000.0)?.toLong()
        }

        val parts = raw.split(':')
        if (parts.size !in 2..3) return null
        val seconds = parts.last().toDoubleOrNull() ?: return null
        val minutes = parts[parts.lastIndex - 1].toLongOrNull() ?: return null
        val hours = if (parts.size == 3) parts[0].toLongOrNull() ?: return null else 0L
        return ((hours * 3_600L + minutes * 60L) * 1_000L + seconds * 1_000.0).toLong()
    }
}

internal object LrcLyricsParser {
    private val timestampRegex = Regex("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")
    private val metadataRegex = Regex("^\\[[A-Za-z]+:.*]$")

    fun parse(lrc: String): Lyrics {
        data class Entry(val startMs: Long, val text: String)

        val entries = buildList {
            lrc.lineSequence().forEach { rawLine ->
                val line = rawLine.trimEnd()
                if (line.isBlank() || metadataRegex.matches(line)) return@forEach
                val timestamps = timestampRegex.findAll(line).toList()
                if (timestamps.isEmpty()) return@forEach
                val text = line.substring(timestamps.last().range.last + 1).trim()
                timestamps.forEach { timestamp ->
                    val minutes = timestamp.groupValues[1].toLong()
                    val seconds = timestamp.groupValues[2].toLong()
                    val fractionText = timestamp.groupValues[3]
                    val fractionMs = when (fractionText.length) {
                        0 -> 0L
                        1 -> fractionText.toLong() * 100L
                        2 -> fractionText.toLong() * 10L
                        else -> fractionText.take(3).padEnd(3, '0').toLong()
                    }
                    add(Entry((minutes * 60L + seconds) * 1_000L + fractionMs, text))
                }
            }
        }.sortedBy(Entry::startMs)

        if (entries.isEmpty()) {
            val plain = lrc.lineSequence()
                .filterNot { metadataRegex.matches(it.trim()) }
                .joinToString("\n") { timestampRegex.replace(it, "") }
                .trim()
            return Lyrics.Simple(plain)
        }

        val lines = entries.mapIndexed { index, entry ->
            val nextStartMs = entries.getOrNull(index + 1)?.startMs
            val endMs = nextStartMs?.coerceAtLeast(entry.startMs) ?: (entry.startMs + 4_000L)
            LineLyric(
                startMs = entry.startMs,
                endMs = endMs,
                text = entry.text,
            )
        }
        return Lyrics.Line(lines)
    }
}

internal object LrcLibLyricsParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseJson(response: String): Lyrics {
        val root = json.parseToJsonElement(response) as? JsonObject
            ?: return Lyrics.Simple("")
        if (root["instrumental"]?.jsonPrimitive?.booleanOrNull == true) {
            return Lyrics.Simple("")
        }
        val synced = root["syncedLyrics"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
        if (synced != null) return LrcLyricsParser.parse(synced)

        val plain = root["plainLyrics"]?.jsonPrimitive?.contentOrNull.orEmpty()
        return Lyrics.Simple(plain)
    }
}

private val kotlinx.serialization.json.JsonPrimitive.contentOrNull: String?
    get() = if (isString) content else content.takeIf { it != "null" }
