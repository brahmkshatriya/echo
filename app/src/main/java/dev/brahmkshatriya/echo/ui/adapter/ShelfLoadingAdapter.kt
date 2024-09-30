package dev.brahmkshatriya.echo.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.databinding.ItemErrorBinding
import dev.brahmkshatriya.echo.databinding.ItemLoginRequiredBinding
import dev.brahmkshatriya.echo.databinding.ItemNotLoadingBinding
import dev.brahmkshatriya.echo.databinding.SkeletonItemContainerBinding
import dev.brahmkshatriya.echo.ui.exception.AppException
import dev.brahmkshatriya.echo.ui.exception.AppException.Companion.toAppException
import dev.brahmkshatriya.echo.ui.exception.ExceptionFragment.Companion.getTitle
import dev.brahmkshatriya.echo.ui.exception.openException
import dev.brahmkshatriya.echo.ui.exception.openLoginException

class ShelfLoadingAdapter(
    private val extension: Extension<*>,
    val listener: Listener? = null
) :
    LoadStateAdapter<ShelfLoadingAdapter.LoadViewHolder>() {

    interface Listener {
        fun onRetry()
        fun onError(view: View, error: Throwable)
        fun onLoginRequired(view: View, error: AppException.LoginRequired)
    }

    class LoadViewHolder(val shelf: Shelf) : RecyclerView.ViewHolder(shelf.root)

    sealed class Shelf(val root: View) {
        data class NotLoading(val binding: ItemNotLoadingBinding, val listener: Listener?) :
            Shelf(binding.root) {
            override fun bind(loadState: LoadState) {
                binding.retry.setOnClickListener {
                    listener?.onRetry()
                }
            }
        }

        data class Loading(val binding: SkeletonItemContainerBinding) : Shelf(binding.root) {
            override fun bind(loadState: LoadState) {}
        }

        data class Error(
            val extension: Extension<*>,
            val binding: ItemErrorBinding,
            val listener: Listener?
        ) :
            Shelf(binding.root) {
            override fun bind(loadState: LoadState) {
                loadState as LoadState.Error
                val appError = loadState.error.toAppException(extension)
                binding.error.run {
                    transitionName = appError.hashCode().toString()
                    text = context.getTitle(appError)
                }
                binding.errorView.setOnClickListener {
                    listener?.onError(binding.error, appError)
                }
                binding.retry.setOnClickListener {
                    listener?.onRetry()
                }
            }
        }

        data class LoginRequired(
            val extension: Extension<*>,
            val binding: ItemLoginRequiredBinding,
            val listener: Listener?
        ) :
            Shelf(binding.root) {
            override fun bind(loadState: LoadState) {
                val error = (loadState as LoadState.Error).error
                val appError = error.toAppException(extension) as AppException.LoginRequired
                binding.error.run {
                    text = context.getTitle(appError)
                }
                binding.login.transitionName = appError.hashCode().toString()
                binding.login.setOnClickListener {
                    listener?.onLoginRequired(it, appError)
                }
            }
        }

        abstract fun bind(loadState: LoadState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return LoadViewHolder(
            when (getStateViewType(loadState)) {

                0 -> Shelf.Loading(
                    SkeletonItemContainerBinding.inflate(inflater, parent, false)
                )

                1 -> Shelf.NotLoading(
                    ItemNotLoadingBinding.inflate(inflater, parent, false), listener
                )

                2 -> Shelf.Error(
                    extension,
                    ItemErrorBinding.inflate(inflater, parent, false),
                    listener
                )

                3 -> Shelf.LoginRequired(
                    extension,
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
                    is ClientException.LoginRequired -> 3
                    else -> 2
                }
            }

        }
    }

    override fun onBindViewHolder(holder: LoadViewHolder, loadState: LoadState) {
        holder.shelf.bind(loadState)
    }

    constructor (fragment: Fragment, extension: Extension<*>, retry: () -> Unit) : this(
        extension,
        object : Listener {
            override fun onRetry() {
                retry()
            }

            override fun onError(view: View, error: Throwable) {
                fragment.requireActivity().openException(error, view)
            }

            override fun onLoginRequired(view: View, error: AppException.LoginRequired) {
                fragment.requireActivity().openLoginException(error, view)
            }
        }
    )
}