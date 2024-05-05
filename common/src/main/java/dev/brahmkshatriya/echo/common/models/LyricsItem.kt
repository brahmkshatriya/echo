package dev.brahmkshatriya.echo.common.models

data class LyricsItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val extras: Map<String, String> = emptyMap()
)
