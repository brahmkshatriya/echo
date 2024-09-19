package dev.brahmkshatriya.echo.common.models

sealed class QuickSearch {
    data class QueryItem(val query: String, val searched: Boolean) : QuickSearch()
    data class MediaItem(val media: EchoMediaItem) : QuickSearch()

    open val title: String
        get() = when (this) {
            is QueryItem -> query
            is MediaItem -> media.title
        }

    fun sameAs(other: QuickSearch): Boolean {
        return when (this) {
            is QueryItem -> other is QueryItem && query == other.query
            is MediaItem -> other is MediaItem && media.sameAs(other.media)
        }
    }
}