package dev.brahmkshatriya.echo.ui.player

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_DOWN
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemPlaylistItemBinding
import dev.brahmkshatriya.echo.player.toTimeString
import dev.brahmkshatriya.echo.utils.loadInto

class PlaylistAdapter(
    val callback: Callback
) : ListAdapter<Pair<Boolean, Track>, PlaylistAdapter.ViewHolder>(DiffCallback()) {

    open class Callback {
        open fun onItemClicked(position: Int) {}
        open fun onDragHandleTouched(viewHolder: ViewHolder) {}
        open fun onItemClosedClicked(position: Int) {}
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class ViewHolder(val binding: ItemPlaylistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.playlistItemClose.setOnClickListener {
                callback.onItemClosedClicked(bindingAdapterPosition)
            }
            binding.playlistItemDragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked != ACTION_DOWN) return@setOnTouchListener false
                callback.onDragHandleTouched(this)
                true
            }
            binding.root.setOnClickListener {
                callback.onItemClicked(bindingAdapterPosition)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Pair<Boolean, Track>>() {
        override fun areItemsTheSame(
            oldItem: Pair<Boolean, Track>,
            newItem: Pair<Boolean, Track>
        ): Boolean = oldItem.second.id == newItem.second.id

        override fun areContentsTheSame(
            oldItem: Pair<Boolean, Track>,
            newItem: Pair<Boolean, Track>
        ): Boolean = oldItem == newItem

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemPlaylistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val item = getItem(position)
        val track = item.second
        binding.playlistItemTitle.text = track.title
        track.cover.loadInto(binding.playlistItemImageView, R.drawable.art_music)
        binding.playlistCurrentItem.isVisible = item.first
        var subtitle = ""
        track.duration?.toTimeString()?.let {
            subtitle += it
        }
        track.artists.joinToString(", ") { it.name }.let {
            if (it.isNotBlank()) subtitle += if (subtitle.isNotBlank()) " • $it" else it
        }
        binding.playlistItemAuthor.isVisible = subtitle.isNotEmpty()
        binding.playlistItemAuthor.text = subtitle
    }


}
