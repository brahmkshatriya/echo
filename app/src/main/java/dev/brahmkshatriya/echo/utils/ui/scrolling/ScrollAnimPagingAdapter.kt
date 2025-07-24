package dev.brahmkshatriya.echo.utils.ui.scrolling

import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.ui.feed.viewholders.FeedViewHolder
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationYAnimation

abstract class ScrollAnimPagingAdapter<T : Any, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>
) : PagingDataAdapter<T, VH>(diffCallback) {

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.itemView.applyTranslationYAnimation(scrollY)
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

    var scrollY: Int = 0
    val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            scrollY = dy
            onEachViewHolder { scrollAmount = dy }
        }
    }

    fun onEachViewHolder(action: FeedViewHolder<*>.() -> Unit) {
        recyclerView?.let { rv ->
            for (i in 0 until rv.childCount) {
                val holder = rv.getChildViewHolder(rv.getChildAt(i)) as? FeedViewHolder<*>
                if (holder?.bindingAdapter == this) holder.action()
            }
        }
    }
}