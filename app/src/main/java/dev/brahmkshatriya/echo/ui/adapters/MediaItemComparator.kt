package dev.brahmkshatriya.echo.ui.adapters

import androidx.recyclerview.widget.DiffUtil
import dev.brahmkshatriya.echo.data.models.MediaItem

object MediaItemComparator : DiffUtil.ItemCallback<MediaItem>() {
    override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
        if (oldItem is MediaItem.TrackItem && newItem is MediaItem.TrackItem)
            return oldItem.track.uri == newItem.track.uri
        if (oldItem is MediaItem.AlbumItem && newItem is MediaItem.AlbumItem)
            return oldItem.album.uri == newItem.album.uri
        if (oldItem is MediaItem.ArtistItem && newItem is MediaItem.ArtistItem)
            return oldItem.artist.uri == newItem.artist.uri
        if (oldItem is MediaItem.PlaylistItem && newItem is MediaItem.PlaylistItem)
            return oldItem.playlist.uri == newItem.playlist.uri
        return false
    }

    override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
        return oldItem == newItem
    }
}