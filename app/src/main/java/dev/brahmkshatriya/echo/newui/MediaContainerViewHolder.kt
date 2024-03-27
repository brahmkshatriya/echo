package dev.brahmkshatriya.echo.newui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.databinding.NewItemMediaCategoryBinding

sealed class MediaContainerViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: MediaItemsContainer)
    open val clickView: View
        get() = itemView
    open val transitionView
        get() = this.clickView

    class Category(val binding: NewItemMediaCategoryBinding) :
        MediaContainerViewHolder(binding.root) {
        override fun bind(item: MediaItemsContainer) {
            val category = item as MediaItemsContainer.Category
            binding.title.text = category.title
            binding.subtitle.text = category.subtitle
            binding.subtitle.isVisible = category.subtitle.isNullOrBlank().not()
            binding.recyclerView.adapter = MediaItemAdapter( category.list)
        }

        override val clickView: View = binding.root

        companion object {
            fun create(parent: ViewGroup ): MediaContainerViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Category(
                    NewItemMediaCategoryBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }
}