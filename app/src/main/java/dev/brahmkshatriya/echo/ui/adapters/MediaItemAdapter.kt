package dev.brahmkshatriya.echo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.data.models.MediaItem
import dev.brahmkshatriya.echo.databinding.ItemMediaBinding
import dev.brahmkshatriya.echo.ui.utils.loadInto

class MediaItemAdapter :
    PagingDataAdapter<MediaItem, MediaItemAdapter.MediaItemHolder>(
        MediaItemComparator
    ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MediaItemHolder(
        ItemMediaBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: MediaItemHolder, position: Int) {
        val item = getItem(position) ?: return
        val binding = holder.binding
        when (item) {
            is MediaItem.TrackItem -> {
                binding.title.text = item.track.title
                item.track.cover?.loadInto(binding.imageView)
            }

            is MediaItem.AlbumItem -> {
                binding.title.text = item.album.title
                item.album.cover?.loadInto(binding.imageView)
            }

            is MediaItem.ArtistItem -> {
                binding.title.text = item.artist.name
                item.artist.cover?.loadInto(binding.imageView)
            }

            is MediaItem.PlaylistItem -> {
                binding.title.text = item.playlist.title
                item.playlist.cover?.loadInto(binding.imageView)
            }
        }
    }

    class MediaItemHolder(val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root)

    companion object MediaItemComparator : DiffUtil.ItemCallback<MediaItem>() {
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
}