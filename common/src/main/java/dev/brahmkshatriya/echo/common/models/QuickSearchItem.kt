package dev.brahmkshatriya.echo.common.models

sealed class QuickSearchItem {
    abstract val searched: Boolean

    data class Query(
        val query: String,
        override val searched: Boolean,
        val extras: Map<String, String> = mapOf()
    ) : QuickSearchItem()

    data class Media(
        val media: EchoMediaItem,
        override val searched: Boolean
    ) : QuickSearchItem()

    open val title: String
        get() = when (this) {
            is Query -> query
            is Media -> media.title
        }

    fun sameAs(other: QuickSearchItem): Boolean {
        return when (this) {
            is Query -> other is Query && query == other.query
            is Media -> other is Media && media.sameAs(other.media)
        }
    }
}