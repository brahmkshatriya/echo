package dev.brahmkshatriya.echo.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.ui.adapter.ShowButtonViewHolder.Companion.ifShowingButton

class ShelfListItemViewAdapter(
    private val clientId: String,
    private val listener: ShelfAdapter.Listener
) : RecyclerView.Adapter<ShelfListItemViewHolder>() {

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

    fun getItem(position: Int) = list?.getOrNull(position)

    var list: List<Any>? = null
    fun submitList(list: List<Any>?) {
        notifyItemRangeRemoved(0, itemCount)
        this.list = list
        notifyItemRangeInserted(0, list?.size ?: 0)
    }

    override fun getItemCount() = list?.size ?: 0

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