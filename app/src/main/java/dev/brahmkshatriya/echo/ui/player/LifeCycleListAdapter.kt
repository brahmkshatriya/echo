package dev.brahmkshatriya.echo.ui.player

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class LifeCycleListAdapter<TData : Any, TBindingType : ViewBinding>(
    diffCallback: DiffUtil.ItemCallback<TData>
) : ListAdapter<TData, LifeCycleListAdapter.Holder<TData, TBindingType>>(diffCallback) {


    abstract fun inflateCallback(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): TBindingType

    abstract fun Holder<TData, TBindingType>.onBind(position: Int)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<TData, TBindingType> {
        val binding = inflateCallback(LayoutInflater.from(parent.context), parent)
        println("onCreateViewHolder: $binding")
        val holder = Holder(binding) { onBind(it) }
        holder.lifecycleRegistry = LifecycleRegistry(holder)
        return holder
    }

    override fun onBindViewHolder(
        holder: Holder<TData, TBindingType>,
        position: Int
    ) {
        destroyLifeCycle(holder)
        holder.lifecycleRegistry = LifecycleRegistry(holder)
        holder.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        holder.bind(position)
    }

    private fun destroyLifeCycle(holder: Holder<TData, TBindingType>) {
        if(holder.lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED))
            holder.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun onViewRecycled(holder: Holder<TData, TBindingType>) {
        super.onViewRecycled(holder)
        destroyLifeCycle(holder)
    }


    class Holder<TData, TBindingType : ViewBinding>(
        val binding: TBindingType,
        private val onBind: Holder<TData, TBindingType>.(position: Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root), LifecycleOwner {

        lateinit var lifecycleRegistry : LifecycleRegistry

        fun bind(position: Int) {
            onBind(position)
        }

        override val lifecycle get() = lifecycleRegistry
    }
}


