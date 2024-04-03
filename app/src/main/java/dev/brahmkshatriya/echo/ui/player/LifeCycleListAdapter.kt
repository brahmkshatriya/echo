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
        return Holder(binding) { onBind(it) }
    }

    override fun onBindViewHolder(
        holder: Holder<TData, TBindingType>,
        position: Int
    ) {
        holder.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        holder.lifecycleRegistry = LifecycleRegistry(holder)
        holder.bind(position)
    }


    class Holder<TData, TBindingType : ViewBinding>(
        val binding: TBindingType,
        private val onBind: Holder<TData, TBindingType>.(position: Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root), LifecycleOwner {

        var lifecycleRegistry = LifecycleRegistry(this)

        fun bind(position: Int) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            onBind(position)
        }

        override val lifecycle get() = lifecycleRegistry
    }
}


