package dev.brahmkshatriya.echo.ui.shelf.adapter.lists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.shelf.adapter.MediaItemViewHolder
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfAdapter
import dev.brahmkshatriya.echo.ui.shelf.adapter.lists.ThreeTrackShelfListsViewHolder.Companion.MULTIPLIER

class ShelfListsAdapter(
    private val listener: ShelfAdapter.Listener
) : RecyclerView.Adapter<ShelfListsAdapter.ViewHolder>() {

    interface Listener : MediaItemViewHolder.Listener {
        fun onCategoryClicked(extensionId: String?, category: Shelf.Category?, view: View)
        fun onCategoryLongClicked(extensionId: String?, category: Shelf.Category?, view: View)
    }

    abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var extensionId: String? = null
        abstract fun bind(shelf: Shelf.Lists<*>?, position: Int, xScroll: Int, yScroll: Int)
        open fun onCurrentChanged(current: PlayerState.Current?) {}
    }

    override fun getItemViewType(position: Int): Int {
        val shelf = shelf ?: error("null shelf")
        return when (val item = shelf.list[position]) {
            is Shelf.Category -> if (shelf.type == Shelf.Lists.Type.Grid) 0 else 1
            is EchoMediaItem -> if (shelf.type == Shelf.Lists.Type.Grid) 3 else 2
            is Track -> if (shelf.type == Shelf.Lists.Type.Grid) 3 else 4
            else -> error("unknown item type: $item")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = when (viewType) {
            0, 1 -> CategoryShelfListsViewHolder(listener, viewType == 0, inflater, parent)
            2 -> MediaItemShelfListsViewHolder(listener, inflater, parent)
            3 -> GridMediaShelfListsViewHolder(listener, inflater, parent)
            4 -> ThreeTrackShelfListsViewHolder(listener, inflater, parent)
            else -> error("unknown viewType: $viewType")
        }
        viewHolder.extensionId = extensionId
        return viewHolder
    }

    var scrollAmountY: Int = 0
    private var extensionId: String? = null
    private var shelf: Shelf.Lists<*>? = null
    fun submit(extensionId: String?, shelf: Shelf.Lists<*>?) {
        notifyItemRangeRemoved(0, itemCount)
        this.extensionId = extensionId
        this.shelf = shelf
        onEachViewHolder { this.extensionId = extensionId }
        notifyItemRangeInserted(0, shelf?.list?.size ?: 0)
    }

    override fun getItemCount(): Int {
        val shelf = shelf
        return if (shelf is Shelf.Lists.Tracks && shelf.type == Shelf.Lists.Type.Linear) {
            (shelf.list.size + MULTIPLIER - 1) / MULTIPLIER
        } else shelf?.list?.size ?: 0
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(shelf, position, scrollAmountX, scrollAmountY)
        holder.onCurrentChanged(current)
    }

    var current: PlayerState.Current? = null
    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.onCurrentChanged(current)
        holder.extensionId = extensionId
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.onCurrentChanged(current)
        holder.extensionId = extensionId
    }

    private var scrollAmountX: Int = 0
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            scrollAmountX = dx
        }
    }

    var recyclerView: RecyclerView? = null
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        recyclerView.addOnScrollListener(scrollListener)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        recyclerView.removeOnScrollListener(scrollListener)
        this.recyclerView = null
    }

    private fun onEachViewHolder(action: ViewHolder.() -> Unit) {
        recyclerView?.let { rv ->
            for (i in 0 until rv.childCount) {
                val holder = rv.getChildViewHolder(rv.getChildAt(i)) as? ViewHolder
                holder?.action()
            }
        }
    }

    fun onCurrentChanged(current: PlayerState.Current?) {
        this.current = current
        onEachViewHolder { onCurrentChanged(current) }
    }
}