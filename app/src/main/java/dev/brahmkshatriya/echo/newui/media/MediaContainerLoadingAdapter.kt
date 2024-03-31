package dev.brahmkshatriya.echo.newui.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.ItemErrorBinding
import dev.brahmkshatriya.echo.databinding.SkeletonItemContainerBinding
import dev.brahmkshatriya.echo.newui.exception.openException

class MediaContainerLoadingAdapter(val listener: Listener? = null) :
    LoadStateAdapter<MediaContainerLoadingAdapter.ShimmerViewHolder>() {

    interface Listener {
        fun onRetry()
        fun onError(view: View, error: Throwable)
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
            listener?.onError(binding.root, loadState.error)
        }
        binding.retry.setOnClickListener {
            listener?.onRetry()
        }
    }

    constructor(fragment: Fragment, retry: () -> Unit) : this(object : Listener {
        override fun onRetry() {
            retry()
        }

        override fun onError(view: View, error: Throwable) {
            fragment.requireActivity().openException(view, error)
        }
    })

    companion object {
        fun MediaContainerAdapter.withLoaders(): ConcatAdapter {
            val footer = MediaContainerLoadingAdapter(fragment) { retry() }
            val header = MediaContainerLoadingAdapter(fragment) { retry() }
            addLoadStateListener { loadStates ->
                header.loadState = loadStates.refresh
                footer.loadState = loadStates.append
            }
            return ConcatAdapter(header, this, footer)
        }
    }
}