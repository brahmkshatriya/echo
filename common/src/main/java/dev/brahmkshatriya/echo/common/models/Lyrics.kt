package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

@Serializable
data class Lyrics(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val lyrics: Lyric? = null,
    val extras: Map<String, String> = emptyMap()
) {

    @Serializable
    sealed class Lyric

    @Serializable
    data class Simple(val text: String) : Lyric()

    @Serializable
    data class Timed(
        val list: List<Item>,
        val fillTimeGaps: Boolean = true
    ) : Lyric()

    @Serializable
    data class Item(
        val text: String,
        val startTime: Long,
        val endTime: Long
    )
}
