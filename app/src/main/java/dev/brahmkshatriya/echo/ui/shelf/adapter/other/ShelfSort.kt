package dev.brahmkshatriya.echo.ui.shelf.adapter.other

import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.ui.shelf.adapter.other.ShelfSort.entries
import kotlinx.serialization.Serializable

fun getSorts(data: List<Shelf>): List<ShelfSort> {
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
        is EchoMediaItem.TrackItem -> item.track.duration
        is EchoMediaItem.Lists.AlbumItem -> item.album.duration
        is EchoMediaItem.Lists.PlaylistItem -> item.playlist.duration
        else -> null
    }

    is Shelf.Lists<*> -> null
}

private fun Shelf.date() = when (this) {
    is Shelf.Category -> null
    is Shelf.Item -> when (val item = this.media) {
        is EchoMediaItem.TrackItem -> item.track.releaseDate
        is EchoMediaItem.Lists.AlbumItem -> item.album.releaseDate
        is EchoMediaItem.Lists.PlaylistItem -> item.playlist.creationDate
        else -> null
    }

    is Shelf.Lists<*> -> null
}

private fun Shelf.copy(subtitle: String) = when (this) {
    is Shelf.Category -> this.copy(subtitle = subtitle)
    is Shelf.Item -> this.copy(media = this.media.copy(subtitle = subtitle))
    is Shelf.Lists<*> -> throw IllegalStateException()
}

enum class ShelfSort(
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
    });

    fun shouldBeVisible(data: List<Shelf>): ShelfSort? {
        val take = when (this) {
            Title -> data.any { it.title()?.isNotEmpty() ?: false }
            Subtitle -> data.any { it.subtitle()?.isNotEmpty() ?: false }
            Date -> data.any { it.date() != null }
            Duration -> data.any { it.duration() != null }
        }
        return if (take) this else null
    }

    @Serializable
    data class State(
        val shelfSort: ShelfSort? = null,
        val reversed: Boolean = false,
        val save: Boolean = false,
    )
}