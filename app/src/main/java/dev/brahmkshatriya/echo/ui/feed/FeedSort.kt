package dev.brahmkshatriya.echo.ui.feed

import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.ui.feed.FeedSort.entries
import kotlinx.serialization.Serializable

fun getSorts(data: List<Shelf>): List<FeedSort> {
    return entries.filter { it.shouldBeVisible(data) != null }
}

private fun Shelf.title() = when (this) {
    is Shelf.Category -> this.title
    is Shelf.Item -> this.media.title
    is Shelf.Lists<*> -> null
}

private fun Shelf.subtitle() = when (this) {
    is Shelf.Category -> this.subtitle
    is Shelf.Item -> this.media.subtitle
    is Shelf.Lists<*> -> null
}

private fun Shelf.duration() = when (this) {
    is Shelf.Category -> null
    is Shelf.Item -> when (val item = this.media) {
        is Track -> item.duration
        is Album -> item.duration
        is Playlist -> item.duration
        else -> null
    }

    is Shelf.Lists<*> -> null
}

private fun Shelf.date() = when (this) {
    is Shelf.Category -> null
    is Shelf.Item -> when (val item = this.media) {
        is Track -> item.releaseDate
        is Album -> item.releaseDate
        is Playlist -> item.creationDate
        else -> null
    }

    is Shelf.Lists<*> -> null
}

private fun Shelf.dateAdded() = when (this) {
    is Shelf.Category -> null
    is Shelf.Item -> when (val item = this.media) {
        is Track -> item.playlistAddedDate
        else -> null
    }

    is Shelf.Lists<*> -> null
}

private fun Shelf.artists() = when (this) {
    is Shelf.Category -> null
    is Shelf.Item -> when (val item = this.media) {
        is Track -> item.artists
        is EchoMediaItem.Lists -> item.artists
        else -> null
    }

    is Shelf.Lists<*> -> null
}

private fun Shelf.album() = when (this) {
    is Shelf.Category -> null
    is Shelf.Item -> when (val item = this.media) {
        is Track -> item.album
        else -> null
    }
    is Shelf.Lists<*> -> null
}

private fun Shelf.copy(subtitle: String) = when (this) {
    is Shelf.Category -> this.copy(subtitle = subtitle)
    is Shelf.Item -> this.copy(media = this.media.copyMediaItem(subtitle = subtitle))
    is Shelf.Lists<*> -> throw IllegalStateException()
}

enum class FeedSort(
    val title: Int,
    val sorter: (List<Shelf>) -> List<Shelf>
) {
    Title(R.string.sort_title, { list -> list.sortedBy { it.title() } }),
    Subtitle(R.string.sort_subtitle, { list -> list.sortedBy { it.subtitle() } }),
    Duration(R.string.sort_duration, { list ->
        list.sortedBy { it.duration() }
    }),
    Date(R.string.sort_date, { list ->
        list.sortedBy { it.date() }
            .filter { it.date() != null }
            .map { it.copy(subtitle = it.date().toString()) }
    }),
    DateAdded(R.string.sort_date_added, { list ->
        list.sortedBy { it.date() }
            .filter { it.date() != null }
            .map { it.copy(subtitle = it.date().toString()) }
    }),
    Artists(R.string.artists, { list ->
        list.flatMap { shelf ->
            shelf.artists().orEmpty().map {
                it to shelf.copy(subtitle = it.name)
            }
        }.sortedBy { it.first.name.lowercase() }.groupBy { it.first.id }
            .values.flatMap { it -> it.map { it.second } }
    }),
    Album(R.string.albums, { list ->
        list.flatMap { shelf ->
            shelf.album()?.let { album ->
                listOf(shelf.copy(subtitle = album.title))
            } ?: emptyList()
        }.sortedBy { it.subtitle() }
    });

    fun shouldBeVisible(data: List<Shelf>): FeedSort? {
        val take = when (this) {
            Title -> data.any { it.title()?.isNotEmpty() ?: false }
            Subtitle -> data.any { it.subtitle()?.isNotEmpty() ?: false }
            Date -> data.any { it.date() != null }
            DateAdded -> data.any { it.dateAdded() != null }
            Duration -> data.any { it.duration() != null }
            Artists -> data.any { it.artists()?.isNotEmpty() ?: false }
            Album -> data.mapNotNull { it.album() }.toSet().size > 1
        }
        return if (take) this else null
    }

    @Serializable
    data class State(
        val feedSort: FeedSort? = null,
        val reversed: Boolean = false,
        val save: Boolean = false,
    )
}