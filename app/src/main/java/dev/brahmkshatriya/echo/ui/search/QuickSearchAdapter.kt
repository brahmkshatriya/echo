package dev.brahmkshatriya.echo.ui.search

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import dev.brahmkshatriya.echo.common.models.QuickSearch

class QuickSearchAdapter(val listener: Listener) :
    ListAdapter<QuickSearch, QuickSearchViewHolder>(diff) {

    interface Listener {
        fun onClick(item: QuickSearch, transitionView: View)
        fun onLongClick(item: QuickSearch, transitionView: View): Boolean
        fun onInsert(item: QuickSearch)
    }

    companion object {
        val diff = object : DiffUtil.ItemCallback<QuickSearch>() {
            override fun areItemsTheSame(oldItem: QuickSearch, newItem: QuickSearch) =
                oldItem.sameAs(newItem)

            override fun areContentsTheSame(oldItem: QuickSearch, newItem: QuickSearch) =
                oldItem == newItem

        }
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is QuickSearch.QueryItem -> 0
        is QuickSearch.MediaItem -> 1
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
    }
}