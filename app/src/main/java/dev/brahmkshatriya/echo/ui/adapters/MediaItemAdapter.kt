package dev.brahmkshatriya.echo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.data.models.MediaItem
import dev.brahmkshatriya.echo.databinding.ItemMediaBinding
import dev.brahmkshatriya.echo.ui.utils.loadInto

class MediaItemAdapter(diffCallback: DiffUtil.ItemCallback<MediaItem>) :
    PagingDataAdapter<MediaItem, MediaItemAdapter.UserViewHolder>(diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = UserViewHolder(
        ItemMediaBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val item = getItem(position) ?: return
        val binding = holder.binding
        when (item) {
            is MediaItem.TrackItem -> {
                binding.title.text = item.track.title
                item.track.cover?.loadInto(binding.imageView)
            }

            is MediaItem.AlbumItem -> {
                binding.title.text = item.album.title
            }

            is MediaItem.ArtistItem -> {
                binding.title.text = item.artist.name
            }

            is MediaItem.PlaylistItem -> {
                binding.title.text = item.playlist.title
            }
        }
    }

    class UserViewHolder(val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root)
}