package dev.brahmkshatriya.echo.ui.feed.viewholders.shelf

import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track

sealed interface ShelfType {
    enum class Enum {
        Category, Media, ThreeTracks
    }

    val extensionId: String
    val tabId: String?
    val type: Enum
    val id: String
    val context: EchoMediaItem?

    class Category(
        override val extensionId: String,
        override val context: EchoMediaItem?,
        override val tabId: String?,
        val category: Shelf.Category,
    ) : ShelfType {
        override val type = Enum.Category
        override val id = category.id
    }

    class Media(
        override val extensionId: String,
        override val context: EchoMediaItem?,
        override val tabId: String?,
        val media: EchoMediaItem,
    ) : ShelfType {
        override val type = Enum.Media
        override val id = media.id
    }

    class ThreeTracks(
        override val extensionId: String,
        override val context: EchoMediaItem?,
        override val tabId: String?,
        val number: Int?,
        val tracks: Triple<Track, Track?, Track?>,
    ) : ShelfType {
        override val type = Enum.ThreeTracks
        override val id = tracks.run { "${first.id}-${second?.id}-${third?.id}" }
    }
}