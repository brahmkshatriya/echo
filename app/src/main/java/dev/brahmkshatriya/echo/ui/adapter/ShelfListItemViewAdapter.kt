package dev.brahmkshatriya.echo.ui.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.ui.adapter.ShowButtonViewHolder.Companion.ifShowingButton

class ShelfListItemViewAdapter(
    private val clientId: String,
    private val transition: String,
    private val listener: ShelfAdapter.Listener,
    val shelf: Shelf.Lists<*>
) : LifeCycleListAdapter<Any, ShelfListItemViewHolder>(DiffCallback) {

    init {
        shelf.ifShowingButton {
            ShowButtonViewHolder.initialize(this, it)
        } ?: submitList(shelf.list)
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

    override fun createHolder(parent: ViewGroup, viewType: Int): ShelfListItemViewHolder {
        return when (viewType) {
            -1 -> GridViewHolder.create(parent, listener, clientId)
            3 -> CategoryViewHolder.create(parent, listener, clientId)
            4 -> TrackViewHolder.create(parent, listener, clientId, null)
            5 -> ShowButtonViewHolder.create(parent)
            else -> MediaItemViewHolder.create(viewType, parent, listener, clientId)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val grid = shelf.type == Shelf.Lists.Type.Grid
        return when (val item = getItem(position)) {
            is EchoMediaItem -> if (!grid) MediaItemViewHolder.getViewType(item) else -1
            is Shelf.Category -> 3
            is Track -> if (!grid) 4 else -1
            is Boolean -> 5
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: ShelfListItemViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.transitionView.transitionName = (transition + item.hashCode()).hashCode().toString()
        holder.adapter = this
        holder.shelf = shelf
        super.onBindViewHolder(holder, position)
    }
}