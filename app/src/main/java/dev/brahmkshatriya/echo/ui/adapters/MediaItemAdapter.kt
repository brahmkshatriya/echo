package dev.brahmkshatriya.echo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemMediaBinding
import dev.brahmkshatriya.echo.utils.loadInto

class MediaItemAdapter(
    private val listener: ClickListener<Track>
) :
    PagingDataAdapter<EchoMediaItem, MediaItemAdapter.MediaItemHolder>(
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
            is EchoMediaItem.TrackItem -> {
                binding.title.text = item.track.title
                item.track.cover?.loadInto(binding.imageView)
                binding.root.setOnClickListener {
                    listener.onClick(item.track)
                }
                binding.root.setOnLongClickListener {
                    listener.onLongClick(item.track)
                    true
                }
            }

            is EchoMediaItem.AlbumItem -> {
                binding.title.text = item.album.title
                item.album.cover?.loadInto(binding.imageView)
            }

            is EchoMediaItem.ArtistItem -> {
                binding.title.text = item.artist.name
                item.artist.cover?.loadInto(binding.imageView)
            }

            is EchoMediaItem.PlaylistItem -> {
                binding.title.text = item.playlist.title
                item.playlist.cover?.loadInto(binding.imageView)
            }
        }
    }

    fun submitData(lifecycle: Lifecycle, list: List<EchoMediaItem>) {
        submitData(lifecycle, PagingData.from(list))
    }

    class MediaItemHolder(val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root)

    companion object MediaItemComparator : DiffUtil.ItemCallback<EchoMediaItem>() {
        override fun areItemsTheSame(oldItem: EchoMediaItem, newItem: EchoMediaItem): Boolean {
            if (oldItem is EchoMediaItem.TrackItem && newItem is EchoMediaItem.TrackItem)
                return oldItem.track.uri == newItem.track.uri
            if (oldItem is EchoMediaItem.AlbumItem && newItem is EchoMediaItem.AlbumItem)
                return oldItem.album.uri == newItem.album.uri
            if (oldItem is EchoMediaItem.ArtistItem && newItem is EchoMediaItem.ArtistItem)
                return oldItem.artist.uri == newItem.artist.uri
            if (oldItem is EchoMediaItem.PlaylistItem && newItem is EchoMediaItem.PlaylistItem)
                return oldItem.playlist.uri == newItem.playlist.uri
            return false
        }

        override fun areContentsTheSame(oldItem: EchoMediaItem, newItem: EchoMediaItem): Boolean {
            return oldItem == newItem
        }
    }
}