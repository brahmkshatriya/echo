package dev.brahmkshatriya.echo.common.models

sealed class QuickSearchItem {
    data class SearchQueryItem(val query: String, val searched: Boolean) : QuickSearchItem()
    data class SearchMediaItem(val mediaItem: EchoMediaItem) : QuickSearchItem()

    open val title: String
        get() = when (this) {
            is SearchQueryItem -> query
            is SearchMediaItem -> mediaItem.title
        }

    fun sameAs(other: QuickSearchItem): Boolean {
        return when (this) {
            is SearchQueryItem -> other is SearchQueryItem && query == other.query
            is SearchMediaItem -> other is SearchMediaItem && mediaItem.sameAs(other.mediaItem)
        }
    }
}