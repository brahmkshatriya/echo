package dev.brahmkshatriya.echo.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class LifeCycleListAdapter<T : Any, Holder : LifeCycleListAdapter.Holder<T>>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, Holder>(diffCallback) {

    abstract fun createHolder(parent: ViewGroup, viewType: Int): Holder

    @CallSuper
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder {
        val holder = createHolder(parent, viewType)
        holder.lifecycleRegistry = LifecycleRegistry(holder)
        return holder
    }

    @CallSuper
    override fun onBindViewHolder(holder: Holder, position: Int) {
        destroyLifeCycle(holder)
        holder.lifecycleRegistry = LifecycleRegistry(holder)
        holder.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        holder.bind(getItem(position))
    }

    private fun destroyLifeCycle(holder: Holder) {
        if (holder.lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED))
            holder.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    @CallSuper
    override fun onViewRecycled(holder: Holder) {
        destroyLifeCycle(holder)
    }

    abstract class Holder<T>(itemView: View) : RecyclerView.ViewHolder(itemView), LifecycleOwner {
        abstract fun bind(item: T)
        open fun onDestroy() {}
        lateinit var lifecycleRegistry: LifecycleRegistry
        override val lifecycle get() = lifecycleRegistry
    }
}


