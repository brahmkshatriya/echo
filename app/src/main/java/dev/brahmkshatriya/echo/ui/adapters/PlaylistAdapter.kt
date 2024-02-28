package dev.brahmkshatriya.echo.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_DOWN
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemPlaylistItemBinding
import dev.brahmkshatriya.echo.player.Global
import dev.brahmkshatriya.echo.player.PlayerHelper.Companion.toTimeString
import dev.brahmkshatriya.echo.utils.loadInto

class PlaylistAdapter(
    val callback: Callback,
    var list: List<Pair<String, Track>> = Global.queue,
) : RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemPlaylistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = list.count()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val track = list[position].second
        binding.playlistItemTitle.text = track.title
        track.cover.loadInto(binding.playlistItemImageView, R.drawable.art_music)
        binding.playlistCurrentItem.isVisible = position == currentPosition
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

    private fun submitList(list: List<Pair<String, Track>>) {
        this.list = list
    }

    private var currentPosition: Int? = null
    fun setCurrent(list: List<Pair<String, Track>>, position: Int?) {
        submitList(list)
        val old = currentPosition
        currentPosition = position
        old?.let { notifyItemChanged(it) }
        currentPosition?.let { notifyItemChanged(it) }
    }

    fun addItem(queue: List<Pair<String, Track>>, index: Int) {
        submitList(queue)
        notifyItemInserted(index)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeItems(queue: List<Pair<String, Track>>) {
        submitList(queue)
        notifyDataSetChanged()
    }
}
