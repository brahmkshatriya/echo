package dev.brahmkshatriya.echo.common.models

sealed class QuickSearchItem {
    data class SearchQueryItem(val query: String, val searched: Boolean) : QuickSearchItem()
    data class SearchMediaItem(val mediaItem: EchoMediaItem) : QuickSearchItem()
}