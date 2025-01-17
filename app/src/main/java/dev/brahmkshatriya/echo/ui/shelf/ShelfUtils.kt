package dev.brahmkshatriya.echo.ui.shelf

import dev.brahmkshatriya.echo.common.models.Date
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf

object ShelfUtils {
    fun changeSubtitle(it: Shelf, subtitle: String) = when (it) {
        is Shelf.Category -> it.copy(subtitle = subtitle)
        is Shelf.Item -> it.copy(media = it.media.copy(subtitle))
        else -> error("???")
    }

    fun title(it: Shelf) = when (it) {
        is Shelf.Category -> it.title.toSortable()
        is Shelf.Item -> it.media.title.toSortable()
        else -> error("???")
    }

    fun subtitle(it: Shelf) = when (it) {
        is Shelf.Category -> it.subtitle.toSortable()
        is Shelf.Item -> it.media.subtitle.toSortable()
        else -> error("???")
    }

    fun date(it: Shelf) = when (it) {
        is Shelf.Category -> null
        is Shelf.Item -> when (val item = it.media) {
            is EchoMediaItem.Lists.AlbumItem -> item.album.releaseDate
            is EchoMediaItem.Lists.PlaylistItem -> item.playlist.creationDate
            is EchoMediaItem.Lists.RadioItem -> null
            is EchoMediaItem.Profile -> null
            is EchoMediaItem.TrackItem -> item.track.releaseDate
        }

        else -> error("???")
    }.toSortable()

    private fun String?.toSortable() = this?.lowercase() ?: ""
    private fun Date?.toSortable() = this ?: Date(0)
    private fun Long?.toSortable() = this ?: 0

    fun duration(it: Shelf): Long {
        val time = when (it) {
            is Shelf.Category -> null
            is Shelf.Item -> when (val item = it.media) {
                is EchoMediaItem.Lists.AlbumItem -> item.album.duration
                is EchoMediaItem.Lists.PlaylistItem -> item.playlist.duration
                is EchoMediaItem.Lists.RadioItem -> null
                is EchoMediaItem.Profile -> null
                is EchoMediaItem.TrackItem -> item.track.duration
            }

            else -> error("???")
        } ?: 0
        return time.toSortable()
    }
}