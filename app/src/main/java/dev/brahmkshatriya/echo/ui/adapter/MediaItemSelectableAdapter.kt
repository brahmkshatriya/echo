package dev.brahmkshatriya.echo.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.databinding.ItemMediaSelectableBinding
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.bind
import dev.brahmkshatriya.echo.utils.dpToPx
import kotlin.math.roundToInt

class MediaItemSelectableAdapter(
    val listener: Listener
) : RecyclerView.Adapter<MediaItemSelectableAdapter.ViewHolder>() {

    fun interface Listener {
        fun onItemSelected(selected: Boolean, item: EchoMediaItem)
    }

    class ViewHolder(val binding: ItemMediaSelectableBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemMediaSelectableBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    private val items = mutableListOf<EchoMediaItem>()
    private val selectedItems = mutableListOf<EchoMediaItem>()
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.binding
        binding.cover.bind(item)
        binding.title.text = item.title
        binding.selected.isVisible = selectedItems.contains(item)
        binding.root.setOnClickListener {
            val selected = !binding.selected.isVisible
            binding.selected.isVisible = selected
            listener.onItemSelected(selected, item)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(items: List<EchoMediaItem>, selectedItems: List<EchoMediaItem>) {
        this.items.clear()
        this.items.addAll(items)
        this.selectedItems.clear()
        this.selectedItems.addAll(selectedItems)
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