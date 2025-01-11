package dev.brahmkshatriya.echo.ui.adapter

import android.graphics.Color.HSVToColor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfCategoryBinding
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.isNightMode

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

        fun View.randomColor(seed: String): Int {
            val hue = seed.hashCode().rem(360)
            val sat = if (context.isNightMode()) (35f + seed.hashCode().rem(25)) / 100 else 0.2f
            val value = if (context.isNightMode()) 0.5f else 0.9f
            val color = HSVToColor(floatArrayOf(hue.toFloat(), sat, value))
            val with = MaterialColors.getColor(this, R.attr.colorSurface)
            return MaterialColors.harmonize(color, with)
        }
    }

    override fun bind(item: Any) {
        if (item !is Shelf.Category) return
        val root = binding.root
        root.setCardBackgroundColor(root.randomColor(item.title))
        binding.title.text = item.title
        binding.title.isSelected = true
        binding.title.setHorizontallyScrolling(true)
        binding.subtitle.text = item.subtitle
        binding.subtitle.isVisible = item.subtitle.isNullOrBlank().not()

        root.setOnClickListener {
            listener.onClick(clientId, item, root)
        }
        root.setOnLongClickListener {
            listener.onLongClick(clientId, item, root)
        }
    }

    override fun onCurrentChanged(current: Current?) {}

    override val transitionView = binding.root
}