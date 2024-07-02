package dev.brahmkshatriya.echo.ui.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.exceptions.LoginRequiredException
import dev.brahmkshatriya.echo.databinding.ItemErrorBinding
import dev.brahmkshatriya.echo.databinding.ItemLoginRequiredBinding
import dev.brahmkshatriya.echo.databinding.ItemNotLoadingBinding
import dev.brahmkshatriya.echo.databinding.SkeletonItemContainerBinding
import dev.brahmkshatriya.echo.ui.exception.ExceptionFragment.Companion.getTitle
import dev.brahmkshatriya.echo.ui.exception.openException
import dev.brahmkshatriya.echo.ui.exception.openLoginException

class MediaContainerLoadingAdapter(val listener: Listener? = null) :
    LoadStateAdapter<MediaContainerLoadingAdapter.LoadViewHolder>() {

    interface Listener {
        fun onRetry()
        fun onError(view: View, error: Throwable)
        fun onLoginRequired(view: View, error: LoginRequiredException)
    }

    class LoadViewHolder(val container: Container) : RecyclerView.ViewHolder(container.root)

    sealed class Container(val root: View) {
        data class NotLoading(val binding: ItemNotLoadingBinding, val listener: Listener?) :
            Container(binding.root) {
            override fun bind(loadState: LoadState) {
                binding.retry.setOnClickListener {
                    listener?.onRetry()
                }
            }
        }

        data class Loading(val binding: SkeletonItemContainerBinding) : Container(binding.root) {
            override fun bind(loadState: LoadState) {}
        }

        data class Error(val binding: ItemErrorBinding, val listener: Listener?) :
            Container(binding.root) {
            override fun bind(loadState: LoadState) {
                loadState as LoadState.Error
                binding.error.run {
                    transitionName = loadState.error.hashCode().toString()
                    text = context.getTitle(loadState.error)
                }
                binding.errorView.setOnClickListener {
                    listener?.onError(binding.error, loadState.error)
                }
                binding.retry.setOnClickListener {
                    listener?.onRetry()
                }
            }
        }

        data class LoginRequired(val binding: ItemLoginRequiredBinding, val listener: Listener?) :
            Container(binding.root) {
            override fun bind(loadState: LoadState) {
                val error =
                    (loadState as LoadState.Error).error as LoginRequiredException
                binding.error.run {
                    text = context.getTitle(loadState.error)
                }
                binding.login.transitionName = error.hashCode().toString()
                binding.login.setOnClickListener {
                    listener?.onLoginRequired(it, error)
                }
            }
        }

        abstract fun bind(loadState: LoadState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        println("creating state view type for  : $loadState")
        return LoadViewHolder(
            when (getStateViewType(loadState)) {

                0 -> Container.Loading(
                    SkeletonItemContainerBinding.inflate(inflater, parent, false)
                )

                1 -> Container.NotLoading(
                    ItemNotLoadingBinding.inflate(inflater, parent, false), listener
                )

                2 -> Container.Error(
                    ItemErrorBinding.inflate(inflater, parent, false),
                    listener
                )

                3 -> Container.LoginRequired(
                    ItemLoginRequiredBinding.inflate(inflater, parent, false),
                    listener
                )

                else -> throw IllegalStateException()
            }

        )
    }

    override fun getStateViewType(loadState: LoadState): Int {
        return when (loadState) {
            is LoadState.Loading -> 0
            is LoadState.NotLoading -> 1
            is LoadState.Error -> {
                when (loadState.error) {
                    is LoginRequiredException -> 3
                    else -> 2
                }
            }

        }
    }

    override fun onBindViewHolder(holder: LoadViewHolder, loadState: LoadState) {
        holder.container.bind(loadState)
    }

    constructor (fragment: Fragment, retry: () -> Unit) : this(object : Listener {
        override fun onRetry() {
            retry()
        }

        override fun onError(view: View, error: Throwable) {
            fragment.requireActivity().openException(error, view)
        }

        override fun onLoginRequired(view: View, error: LoginRequiredException) {
            fragment.requireActivity().openLoginException(error, view)
        }
    })
}