package dev.brahmkshatriya.echo.utils.ui.scrolling

import androidx.annotation.CallSuper
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationYAnimation

abstract class ScrollAnimLoadStateAdapter<T: ScrollAnimViewHolder> : LoadStateAdapter<T>() {

    @CallSuper
    override fun onBindViewHolder(holder: T, loadState: LoadState) {
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

    @Suppress("UNCHECKED_CAST")
    fun onEachViewHolder(action: T.() -> Unit) {
        recyclerView?.let { rv ->
            for (i in 0 until rv.childCount) {
                val holder = rv.getChildViewHolder(rv.getChildAt(i)) as? T
                if (holder?.bindingAdapter == this) holder.action()
            }
        }
    }
}