package dev.brahmkshatriya.echo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfCategoryBinding

class CategoryViewHolder(
    val listener: ShelfAdapter.Listener,
    val clientId: String,
    val binding: ItemShelfCategoryBinding
) : ShelfListItemViewHolder(binding.root) {

    companion object {
        fun create(
            parent: ViewGroup,
            listener: ShelfAdapter.Listener,
            clientId: String
        ): CategoryViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return CategoryViewHolder(
                listener,
                clientId,
                ItemShelfCategoryBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun bind(item:Any) {
        if (item !is Shelf.Category) return
        val root = binding.root
        root.setCardBackgroundColor(MaterialColors.getColor(root, R.attr.colorSurfaceVariant))
        root.updateLayoutParams { width = ViewGroup.LayoutParams.WRAP_CONTENT }
        binding.title.text = item.title
        binding.subtitle.text = item.subtitle
        binding.subtitle.isVisible = item.subtitle.isNullOrBlank().not()

        root.setOnClickListener {
            listener.onClick(clientId, item, root)
        }
        root.setOnLongClickListener {
            listener.onLongClick(clientId, item, root)
        }
    }

    override val transitionView = binding.root
}