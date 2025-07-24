package dev.brahmkshatriya.echo.ui.playlist.save

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.databinding.ItemShelfMediaBinding
import dev.brahmkshatriya.echo.ui.common.GridAdapter
import dev.brahmkshatriya.echo.ui.feed.viewholders.MediaViewHolder.Companion.bind
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimListAdapter

class MediaItemAdapter(
    private val listener: Listener,
) : ScrollAnimListAdapter<MediaItemAdapter.Item, MediaItemAdapter.ViewHolder>(
    DiffCallback
), GridAdapter {

    data class Item(val extensionId: String, val item: EchoMediaItem) {
        val id = extensionId + item.id
    }

    object DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }

    fun interface Listener {
        fun onMediaItemClicked(view: View?, item: Item?)
    }

    class ViewHolder(
        parent: ViewGroup,
        private val listener: Listener,
        val binding: ItemShelfMediaBinding = ItemShelfMediaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    ) : RecyclerView.ViewHolder(binding.root) {
        var item: Item? = null

        init {
            binding.coverContainer.cover.clipToOutline = true
            binding.root.setOnClickListener {
                listener.onMediaItemClicked(it, item)
            }
            binding.more.isVisible = false
        }

        fun bind(item: Item) {
            this.item = item
            binding.bind(item.item)
            binding.play.isVisible = false
        }
    }

    override val adapter = this
    override fun getSpanSize(position: Int, width: Int, count: Int) = count
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent, listener)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val item = getItem(position) ?: return
        holder.bind(item)
    }
}