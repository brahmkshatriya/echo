package dev.brahmkshatriya.echo.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.databinding.ItemMediaSelectableBinding
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.bind
import dev.brahmkshatriya.echo.utils.dpToPx
import kotlin.math.roundToInt

class MediaItemSelectableAdapter(
    val listener: Listener
) : LifeCycleListAdapter<Pair<EchoMediaItem, Boolean>, MediaItemSelectableAdapter.ViewHolder>(
    DiffCallback
) {

    object DiffCallback : DiffUtil.ItemCallback<Pair<EchoMediaItem, Boolean>>() {

        override fun areItemsTheSame(
            oldItem: Pair<EchoMediaItem, Boolean>, newItem: Pair<EchoMediaItem, Boolean>
        ) = oldItem.first.id == newItem.first.id


        override fun areContentsTheSame(
            oldItem: Pair<EchoMediaItem, Boolean>, newItem: Pair<EchoMediaItem, Boolean>
        ) = oldItem == newItem

    }

    fun interface Listener {
        fun onItemSelected(selected: Boolean, item: EchoMediaItem)
    }

    override fun createHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemMediaSelectableBinding.inflate(inflater, parent, false))
    }

    inner class ViewHolder(val binding: ItemMediaSelectableBinding) :
        Holder<Pair<EchoMediaItem, Boolean>>(binding.root) {
        override fun bind(item: Pair<EchoMediaItem, Boolean>) {
            val mediaItem = item.first
            binding.cover.bind(mediaItem)
            binding.title.text = mediaItem.title
            binding.selected.isVisible = item.second
            binding.root.setOnClickListener {
                val selected = !binding.selected.isVisible
                binding.selected.isVisible = selected
                listener.onItemSelected(selected, mediaItem)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(items: List<EchoMediaItem>, selectedItems: List<EchoMediaItem>) {
        submitList(items.map { it to selectedItems.contains(it) })
        notifyDataSetChanged()
    }

    companion object {
        fun View.mediaItemSpanCount(horizontalPadding: Int = 24 * 2, block: (count: Int) -> Unit) =
            doOnLayout {
                val typed = context.theme.obtainStyledAttributes(intArrayOf(R.attr.itemCoverSize))
                val itemWidth = typed.getDimensionPixelSize(typed.getIndex(0), 0)
                val newWidth = width - horizontalPadding.dpToPx(context)
                val count =
                    (newWidth.toFloat() / (itemWidth + 16.dpToPx(context))).roundToInt()
                block(count)
                requestLayout()
            }
    }

}