package dev.brahmkshatriya.echo.ui.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.ui.adapter.ShowButtonViewHolder.Companion.ifShowingButton

class ShelfListItemViewAdapter(
    private val clientId: String,
    private val listener: ShelfAdapter.Listener
) : ListAdapter<Any, ShelfListItemViewHolder>(DiffCallback) {

    var transition: String = ""
    var shelf: Shelf.Lists<*>? = null
        set(value) {
            field = value
            value?.let {
                shelf?.ifShowingButton {
                    ShowButtonViewHolder.initialize(this, it)
                } ?: submitList(shelf?.list)
            }
        }

    object DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is EchoMediaItem && newItem is EchoMediaItem -> oldItem.sameAs(newItem)
                oldItem is Shelf.Category && newItem is Shelf.Category -> oldItem.sameAs(newItem)
                oldItem is Track && newItem is Track -> oldItem.id == newItem.id
                else -> false
            }
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return oldItem == newItem
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        -1 -> GridViewHolder.create(parent, listener, clientId)
        3, 6 -> CategoryViewHolder.create(viewType == 3, parent, listener, clientId)
        4 -> TrackViewHolder.create(parent, listener, clientId, null)
        5 -> ShowButtonViewHolder.create(parent)
        else -> MediaItemViewHolder.create(viewType, parent, listener, clientId)
    }

    override fun getItemViewType(position: Int): Int {
        val grid = shelf?.type == Shelf.Lists.Type.Grid
        return when (val item = getItem(position)) {
            is EchoMediaItem -> if (!grid) MediaItemViewHolder.getViewType(item) else -1
            is Shelf.Category -> if (grid) 3 else 6
            is Track -> if (!grid) 4 else -1
            is Boolean -> 5
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: ShelfListItemViewHolder, position: Int) {
        holder.shelf = shelf ?: return
        val item = getItem(position) ?: return
        holder.transitionView.transitionName = (transition + item.hashCode()).hashCode().toString()
        holder.adapter = this
        holder.bind(item)
        holder.onCurrentChanged(current)
    }

    private var current: Current? = null
    fun onCurrentChanged(recycler: RecyclerView, current: Current?) {
        this.current = current
        for (i in 0 until recycler.childCount) {
            val holder =
                recycler.getChildViewHolder(recycler.getChildAt(i)) as? ShelfListItemViewHolder
            holder?.onCurrentChanged(current)
        }
    }
}