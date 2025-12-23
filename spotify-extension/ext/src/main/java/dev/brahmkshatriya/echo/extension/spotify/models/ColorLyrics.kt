package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
data class ColorLyrics(
    val lyrics: Lyrics? = null,
    val colors: Colors? = null,
    val hasVocalRemoval: Boolean? = null
) {

    @Serializable
    data class Colors(
        val background: Long? = null,
        val text: Long? = null,
        val highlightText: Long? = null
    )

    @Serializable
    data class Lyrics(
        val syncType: String? = null,
        val lines: List<Line>? = null,
        val provider: String? = null,
        val providerLyricsId: String? = null,
        val providerDisplayName: String? = null,
        val syncLyricsUri: String? = null,
        val isDenseTypeface: Boolean? = null,
        val alternatives: JsonArray? = null,
        val language: String? = null,
        val isRtlLanguage: Boolean? = null,
        val showUpsell: Boolean? = null,
        val capStatus: String? = null,
        val isSnippet: Boolean? = null
    )

    @Serializable
    data class Line(
        val startTimeMs: String? = null,
        val words: String? = null,
        val syllables: JsonArray? = null,
        val endTimeMs: String? = null
    )
}