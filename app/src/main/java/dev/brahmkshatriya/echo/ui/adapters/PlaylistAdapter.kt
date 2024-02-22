package dev.brahmkshatriya.echo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.ItemPlaylistItemBinding
import dev.brahmkshatriya.echo.player.Global
import dev.brahmkshatriya.echo.ui.utils.loadInto

class PlaylistAdapter(
    val callback: Callback
) : RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {

    val list = Global.queue

    open class Callback {
        open fun onItemClicked(position: Int) {}
        open fun onDragHandleClicked(viewHolder: ViewHolder) {}
        open fun onItemClosedClicked(position: Int) {}
    }

    inner class ViewHolder(val binding: ItemPlaylistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.playlistItemClose.setOnClickListener {
                callback.onItemClosedClicked(bindingAdapterPosition)
            }
            binding.playlistItemDragHandle.setOnClickListener {
                callback.onDragHandleClicked(this)
            }
            binding.root.setOnClickListener {
                callback.onItemClicked(bindingAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemPlaylistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = list.count()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val track = list[position].second
        binding.playlistItemTitle.text = track.title
        track.cover?.loadInto(binding.playlistItemImageView)
        binding.playlistItemAuthor.text = track.artists.joinToString(", ") { it.name }
        binding.playlistCurrentItem.isVisible = holder == oldViewHolder
    }

    private var oldViewHolder: ViewHolder? = null
    fun setCurrent(viewHolder: ViewHolder?) {
        oldViewHolder?.binding?.playlistCurrentItem?.isVisible = false
        oldViewHolder = viewHolder
        viewHolder?.binding?.playlistCurrentItem?.apply {
            isVisible = true
        }
    }
}
