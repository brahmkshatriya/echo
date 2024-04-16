package dev.brahmkshatriya.echo.ui.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.exceptions.LoginRequiredException
import dev.brahmkshatriya.echo.databinding.ItemErrorBinding
import dev.brahmkshatriya.echo.databinding.ItemLoginRequiredBinding
import dev.brahmkshatriya.echo.databinding.ItemNotLoadingBinding
import dev.brahmkshatriya.echo.databinding.SkeletonItemContainerBinding
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.exception.openException
import dev.brahmkshatriya.echo.ui.login.LoginFragment

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
                binding.error.text = loadState.error.localizedMessage
                binding.error.transitionName = loadState.error.hashCode().toString()
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
        return LoadViewHolder(
            when (loadState) {
                is LoadState.Error -> {
                    when (loadState.error) {
                        is LoginRequiredException -> Container.LoginRequired(
                            ItemLoginRequiredBinding.inflate(inflater, parent, false),
                            listener
                        )

                        else -> Container.Error(
                            ItemErrorBinding.inflate(inflater, parent, false),
                            listener
                        )
                    }
                }

                is LoadState.Loading -> Container.Loading(
                    SkeletonItemContainerBinding.inflate(inflater, parent, false)
                )

                is LoadState.NotLoading -> Container.NotLoading(
                    ItemNotLoadingBinding.inflate(inflater, parent, false), listener
                )
            }

        )
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
            fragment.requireActivity().openFragment(
                LoginFragment.newInstance(error.clientId, error.clientName),
                view
            )
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