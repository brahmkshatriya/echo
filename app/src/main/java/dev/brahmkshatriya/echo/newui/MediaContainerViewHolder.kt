package dev.brahmkshatriya.echo.newui

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.databinding.NewItemMediaCategoryBinding
import java.lang.ref.WeakReference

sealed class MediaContainerViewHolder(
    itemView: View,
) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: MediaItemsContainer)
    open val clickView = itemView
    abstract val transitionView: View


    class Category(
        val binding: NewItemMediaCategoryBinding,
        val viewModel: MediaContainerAdapter.StateViewModel,
        val listener: MediaItemAdapter.Listener,
    ) :
        MediaContainerViewHolder(binding.root) {
        override fun bind(item: MediaItemsContainer) {
            val category = item as MediaItemsContainer.Category
            binding.title.text = category.title
            binding.subtitle.text = category.subtitle
            binding.subtitle.isVisible = category.subtitle.isNullOrBlank().not()
            binding.recyclerView.adapter = MediaItemAdapter(listener, category.list)
            val position = bindingAdapterPosition
            binding.recyclerView.layoutManager?.apply {
                val state: Parcelable? = viewModel.layoutManagerStates[position]
                if (state != null) onRestoreInstanceState(state)
                else scrollToPosition(0)
            }
            viewModel.visibleScrollableViews[position] = WeakReference(this)
            binding.more.isVisible = category.more != null
        }

        val layoutManager get() = binding.recyclerView.layoutManager
        override val clickView: View = binding.more
        override val transitionView: View = binding.titleCard

        companion object {
            fun create(
                parent: ViewGroup,
                viewModel: MediaContainerAdapter.StateViewModel,
                listener: MediaItemAdapter.Listener,
            ): MediaContainerViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Category(
                    NewItemMediaCategoryBinding.inflate(layoutInflater, parent, false),
                    viewModel,
                    listener
                )
            }
        }
    }
}