package dev.brahmkshatriya.echo.ui.feed

import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension.Companion.EXTENSION_ID
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("clazzType")
@Serializable
sealed interface FeedType {

    enum class Enum {
        Header, HorizontalList,
        Category, CategoryGrid,
        Media, MediaGrid,
        Video, VideoHorizontal,
    }

    val feedId: String
    val id: String
    val type: Enum
    val extId: String
    val extras: Map<String, String>?

    val extensionId: String
        get() = extras?.let {
            if (it["cached"] == "true") it[EXTENSION_ID] else null
        } ?: extId

    val context: EchoMediaItem?
    val tabId: String?

    @Serializable
    data class Header(
        override val feedId: String,
        override val extId: String,
        override val context: EchoMediaItem?,
        override val tabId: String?,
        override val id: String,
        val title: String,
        val subtitle: String? = null,
        @Transient val more: Feed<Shelf>? = null,
        val tracks: List<Track>? = null,
    ) : FeedType {
        override val type = Enum.Header
        override val extras: Map<String, String>? = null
    }

    @Serializable
    data class Category(
        override val feedId: String,
        override val extId: String,
        override val context: EchoMediaItem?,
        override val tabId: String?,
        val shelf: Shelf.Category,
        override val type: Enum = Enum.Category,
    ) : FeedType {
        override val id = shelf.id
        override val extras: Map<String, String>? = shelf.extras
    }

    @Serializable
    data class Media(
        override val feedId: String,
        override val extId: String,
        override val context: EchoMediaItem?,
        override val tabId: String?,
        val item: EchoMediaItem,
        val number: Long?,
    ) : FeedType {
        override val id = item.id
        override val type: Enum = Enum.Media
        override val extras: Map<String, String>? = item.extras
    }

    @Serializable
    data class Video(
        override val feedId: String,
        override val extId: String,
        override val context: EchoMediaItem?,
        override val tabId: String?,
        val item: Track,
        override val type: Enum = Enum.Video,
    ) : FeedType {
        override val id = item.id
        override val extras: Map<String, String>? = item.extras
    }

    @Serializable
    data class MediaGrid(
        override val feedId: String,
        override val extId: String,
        override val context: EchoMediaItem?,
        override val tabId: String?,
        val item: EchoMediaItem,
        val number: Int? = null,
    ) : FeedType {
        override val id = item.id
        override val type: Enum = Enum.MediaGrid
        override val extras: Map<String, String>? = item.extras
    }

    @Serializable
    data class HorizontalList(
        override val feedId: String,
        override val extId: String,
        override val context: EchoMediaItem?,
        override val tabId: String?,
        val shelf: Shelf.Lists<*>,
    ) : FeedType {
        override val id = shelf.id
        override val type = Enum.HorizontalList
        override val extras: Map<String, String>? = shelf.extras
    }

    companion object {
        fun List<Shelf>.toFeedType(
            feedId: String,
            extId: String,
            context: EchoMediaItem?,
            tabId: String?,
            noVideos: Boolean = false,
            start: Long = 0,
        ): List<FeedType> = mapIndexed { index, shelf ->

            when (shelf) {
                is Shelf.Category -> if (shelf.feed == null) listOf(
                    Header(
                        feedId, extId, context, tabId, shelf.id, shelf.title, shelf.subtitle,
                    )
                ) else listOf(Category(feedId, extId, context, tabId, shelf))

                is Shelf.Item -> when (val item = shelf.media) {
                    is Track -> if (!noVideos) when (item.type) {
                        Track.Type.Video -> listOf(Video(feedId, extId, context, tabId, item))
                        Track.Type.HorizontalVideo -> listOf(
                            Video(feedId, extId, context, tabId, item, Enum.VideoHorizontal)
                        )

                        else -> listOf(Media(feedId, extId, context, tabId, item, null))
                    } else {
                        val index = start + index
                        listOf(Media(feedId, extId, context, tabId, item, index))
                    }

                    else -> listOf(Media(feedId, extId, context, tabId, item, null))
                }

                is Shelf.Lists<*> -> listOf(
                    Header(
                        feedId, extId, context, tabId,
                        shelf.id, shelf.title, shelf.subtitle, shelf.more,
                        if (shelf is Shelf.Lists.Tracks) shelf.list else null
                    )
                ) + if (shelf.type == Shelf.Lists.Type.Linear) listOf(
                    HorizontalList(feedId, extId, context, tabId, shelf)
                )
                else when (shelf) {
                    is Shelf.Lists.Categories -> shelf.list.map {
                        Category(feedId, extId, context, tabId, it, Enum.CategoryGrid)
                    }

                    is Shelf.Lists.Items -> shelf.list.map {
                        MediaGrid(feedId, extId, context, tabId, it)
                    }

                    is Shelf.Lists.Tracks -> shelf.list.mapIndexed { index, item ->
                        MediaGrid(feedId, extId, context, tabId, item, index + 1)
                    }
                }
            }
        }.flatten()
    }
}