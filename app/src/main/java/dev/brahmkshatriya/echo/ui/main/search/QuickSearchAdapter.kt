package dev.brahmkshatriya.echo.ui.search

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import dev.brahmkshatriya.echo.common.models.QuickSearchItem

class QuickSearchAdapter(val listener: Listener) :
    ListAdapter<QuickSearchItem, QuickSearchViewHolder>(diff) {

    interface Listener {
        fun onClick(item: QuickSearchItem, transitionView: View)
        fun onLongClick(item: QuickSearchItem, transitionView: View): Boolean
        fun onInsert(item: QuickSearchItem)
        fun onDeleteClick(item: QuickSearchItem)
    }

    companion object {
        val diff = object : DiffUtil.ItemCallback<QuickSearchItem>() {
            override fun areItemsTheSame(oldItem: QuickSearchItem, newItem: QuickSearchItem) =
                oldItem.sameAs(newItem)

            override fun areContentsTheSame(oldItem: QuickSearchItem, newItem: QuickSearchItem) =
                oldItem == newItem

        }
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
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

        holder.deleteView.isVisible = item.searched
        holder.deleteView.setOnClickListener {
            listener.onDeleteClick(item)
        }
    }
}