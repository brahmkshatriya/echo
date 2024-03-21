package dev.brahmkshatriya.echo.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.ItemErrorBinding
import dev.brahmkshatriya.echo.databinding.SkeletonItemContainerBinding

class ContainerLoadingAdapter(val listener: ContainerListener? = null) :
    LoadStateAdapter<ContainerLoadingAdapter.ShimmerViewHolder>() {

    interface ContainerListener {
        fun onRetry()
        fun onError(error: Throwable)
    }

    class ShimmerViewHolder(val container: Container) :
        RecyclerView.ViewHolder(container.root)

    sealed class Container(val root: View) {
        data class Loading(val binding: SkeletonItemContainerBinding) : Container(binding.root)
        data class Error(val binding: ItemErrorBinding) : Container(binding.root)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): ShimmerViewHolder {
        return ShimmerViewHolder(
            when (loadState) {
                is LoadState.Error, is LoadState.NotLoading -> {
                    Container.Error(
                        ItemErrorBinding.inflate(
                            LayoutInflater.from(parent.context), parent, false
                        )
                    )
                }

                is LoadState.Loading -> {
                    Container.Loading(
                        SkeletonItemContainerBinding.inflate(
                            LayoutInflater.from(parent.context), parent, false
                        )
                    )
                }
            }
        )
    }

    override fun onBindViewHolder(holder: ShimmerViewHolder, loadState: LoadState) {
        if (loadState !is LoadState.Error) return

        val binding = (holder.container as Container.Error).binding
        binding.error.text = loadState.error.localizedMessage
        binding.errorView.setOnClickListener {
            listener?.onError(loadState.error)
        }
        binding.retry.setOnClickListener {
            listener?.onRetry()
        }
    }
}