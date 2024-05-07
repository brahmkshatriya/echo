package dev.brahmkshatriya.echo.common.models

data class Lyrics(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val lyrics: List<Lyric>? = null,
    val fillTimeGaps : Boolean = true,
    val extras: Map<String, String> = emptyMap()
)
