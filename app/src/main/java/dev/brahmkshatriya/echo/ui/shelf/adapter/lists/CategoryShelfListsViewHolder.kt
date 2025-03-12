package dev.brahmkshatriya.echo.ui.shelf.adapter.lists

import android.graphics.Color.HSVToColor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfCategoryBinding
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfAdapter
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationAndScaleAnimation
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationYAnimation
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isNightMode
import dev.brahmkshatriya.echo.utils.ui.UiUtils.marquee

class CategoryShelfListsViewHolder(
    listener: ShelfAdapter.Listener,
    private val matchParent: Boolean,
    inflater: LayoutInflater,
    parent: ViewGroup,
    val binding: ItemShelfCategoryBinding =
        ItemShelfCategoryBinding.inflate(inflater, parent, false)
) : ShelfListsAdapter.ViewHolder(binding.root) {

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
        binding.root.updateLayoutParams<MarginLayoutParams> {
            if (!matchParent) width = 112.dpToPx(binding.root.context)
            marginStart = 4.dpToPx(binding.root.context)
            marginEnd = marginStart
        }
    }

    companion object {
        fun View.randomColor(seed: String?): Int {
            val hue = seed.hashCode().rem(360)
            val sat = if (context.isNightMode()) (35f + seed.hashCode().rem(25)) / 100 else 0.2f
            val value = if (context.isNightMode()) 0.5f else 0.9f
            val color = HSVToColor(floatArrayOf(hue.toFloat(), sat, value))
            val with = MaterialColors.getColor(this, R.attr.echoBackground)
            return MaterialColors.harmonize(color, with)
        }
    }

    override fun bind(shelf: Shelf.Lists<*>?, position: Int, xScroll: Int, yScroll: Int) {
        val categories = shelf as? Shelf.Lists.Categories ?: return
        val category = categories.list.getOrNull(position) ?: return
        this.category = category
        binding.root.apply {
            setCardBackgroundColor(randomColor(category.id))
        }
        binding.title.text = category.title
        binding.subtitle.text = category.subtitle
        binding.subtitle.isVisible = !category.subtitle.isNullOrBlank()
        binding.root.isClickable = category.items != null
        if (!matchParent) binding.root.applyTranslationAndScaleAnimation(xScroll)
        else binding.root.applyTranslationYAnimation(yScroll)
    }
}