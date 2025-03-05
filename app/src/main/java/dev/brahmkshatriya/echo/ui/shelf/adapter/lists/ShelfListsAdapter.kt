package dev.brahmkshatriya.echo.ui.shelf.adapter.lists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfAdapter
import dev.brahmkshatriya.echo.ui.shelf.adapter.TrackAdapter
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyScaleAnimation

class ShelfListsAdapter(
    private val listener: ShelfAdapter.Listener
) : RecyclerView.Adapter<ShelfListsAdapter.ViewHolder>() {

    interface Listener : TrackAdapter.Listener {
        fun onMediaItemClicked(extensionId: String?, item: EchoMediaItem?, it: View)
        fun onMediaItemLongClicked(extensionId: String?, item: EchoMediaItem?, it: View)

        fun onCategoryClicked(extensionId: String?, category: Shelf.Category?, view: View)
        fun onCategoryLongClicked(extensionId: String?, category: Shelf.Category?, view: View)
    }

    abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var extensionId: String? = null
        abstract fun bind(shelf: Shelf.Lists<*>?, position: Int)
        open fun onCurrentChanged(current: PlayerState.Current?) {}
    }

    override fun getItemViewType(position: Int): Int {
        val shelf = shelf ?: error("null shelf")
        return when (val item = shelf.list[position]) {
            is EchoMediaItem -> 1
            is Shelf.Category -> 2
            is Track -> 3
            else -> error("unknown shelf item: $item")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = when (viewType) {
            1 -> MediaItemShelfListsViewHolder.create(listener, inflater, parent)
            else -> CategoryShelfListsViewHolder.create(listener, inflater, parent)
        }
        viewHolder.extensionId = extensionId
        return viewHolder
    }

    private var extensionId: String? = null
    private var shelf: Shelf.Lists<*>? = null
    fun submit(extensionId: String?, shelf: Shelf.Lists<*>?) {
        notifyItemRangeRemoved(0, itemCount)
        this.extensionId = extensionId
        this.shelf = shelf
        onEachViewHolder { this.extensionId = extensionId }
        notifyItemRangeInserted(0, shelf?.list?.size ?: 0)
    }

    override fun getItemCount() = shelf?.list?.size ?: 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(shelf, position)
        holder.onCurrentChanged(current)
        holder.itemView.applyScaleAnimation()
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

    var recyclerView: RecyclerView? = null
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
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