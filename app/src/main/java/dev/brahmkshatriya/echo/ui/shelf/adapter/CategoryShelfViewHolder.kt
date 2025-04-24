package dev.brahmkshatriya.echo.ui.shelf.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfCategoryBinding
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationYAnimation
import dev.brahmkshatriya.echo.utils.ui.UiUtils.marquee

class CategoryShelfViewHolder(
    val listener: ShelfAdapter.Listener,
    val binding: ItemShelfCategoryBinding
) : ShelfAdapter.ViewHolder(binding.root) {

    private var category: Shelf.Category? = null

    init {
        binding.root.setOnClickListener {
            listener.onCategoryClicked(extensionId, category, it)
        }
        binding.root.setOnLongClickListener {
            listener.onCategoryLongClicked(extensionId, category, it)
            true
        }
        binding.title.marquee()
    }

    override fun bind(item: Shelf?) {
        val category = item as? Shelf.Category ?: return
        this.category = category
        binding.title.text = category.title
        binding.subtitle.text = category.subtitle
        binding.subtitle.isVisible = !category.subtitle.isNullOrBlank()
        binding.root.isClickable = category.items != null
        binding.root.setCardBackgroundColor(Color.TRANSPARENT)
        binding.root.applyTranslationYAnimation(scrollAmount)
    }

    companion object {
        fun create(
            listener: ShelfAdapter.Listener, inflater: LayoutInflater, parent: ViewGroup
        ): CategoryShelfViewHolder {
            val binding = ItemShelfCategoryBinding.inflate(inflater, parent, false)
            return CategoryShelfViewHolder(listener, binding)
        }
    }
}
