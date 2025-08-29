package dev.brahmkshatriya.echo.ui.main.search

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimListAdapter

class QuickSearchAdapter(
    val listener: Listener,
) : ScrollAnimListAdapter<QuickSearchAdapter.Item, QuickSearchViewHolder>(DiffCallback) {
    data class Item(
        val extensionId: String,
        val actual: QuickSearchItem,
    )

    object DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            if (oldItem.extensionId != newItem.extensionId) return false
            return oldItem.actual.sameAs(newItem.actual)
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }

    interface Listener {
        fun onClick(item: Item, transitionView: View)
        fun onLongClick(item: Item, transitionView: View): Boolean
        fun onInsert(item: Item)
        fun onDeleteClick(item: Item)
    }

    override fun getItemViewType(position: Int) = when (getItem(position).actual) {
        is QuickSearchItem.Query -> 0
        is QuickSearchItem.Media -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        0 -> QuickSearchViewHolder.Query.create(parent)
        1 -> QuickSearchViewHolder.Media.create(parent)
        else -> throw IllegalArgumentException("Unknown viewType: $viewType")
    }

    override fun onBindViewHolder(holder: QuickSearchViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(item)
        holder.itemView.setOnClickListener {
            listener.onClick(item, holder.transitionView)
        }
        holder.itemView.setOnLongClickListener {
            listener.onLongClick(item, holder.transitionView)
        }
        holder.insertView.setOnClickListener {
            listener.onInsert(item)
        }

        holder.deleteView.isVisible = item.actual.searched
        holder.deleteView.setOnClickListener {
            listener.onDeleteClick(item)
        }
    }
}