package dev.brahmkshatriya.echo.data.models

sealed class QuickSearchItem {
    data class SearchQueryItem(val query: String) : QuickSearchItem()
    data class SearchMediaItem(val mediaItem: EchoMediaItem) : QuickSearchItem()
}