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
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.PlayerState.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfAdapter
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isNightMode
import dev.brahmkshatriya.echo.utils.ui.UiUtils.marquee

class CategoryShelfListsViewHolder(
    val listener: ShelfAdapter.Listener,
    val binding: ItemShelfCategoryBinding
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
            width = 112.dpToPx(binding.root.context)
            marginStart = 4.dpToPx(binding.root.context)
            marginEnd = marginStart
        }
    }

    companion object {
        fun create(
            listener: ShelfAdapter.Listener, inflater: LayoutInflater, parent: ViewGroup
        ): CategoryShelfListsViewHolder {
            val binding = ItemShelfCategoryBinding.inflate(inflater, parent, false)
            return CategoryShelfListsViewHolder(listener, binding)
        }

        fun View.randomColor(seed: String?): Int {
            val hue = seed.hashCode().rem(360)
            val sat = if (context.isNightMode()) (35f + seed.hashCode().rem(25)) / 100 else 0.2f
            val value = if (context.isNightMode()) 0.5f else 0.9f
            val color = HSVToColor(floatArrayOf(hue.toFloat(), sat, value))
            val with = MaterialColors.getColor(this, R.attr.echoBackground)
            return MaterialColors.harmonize(color, with)
        }
    }

    var itemId: String? = null
    override fun bind(shelf: Shelf.Lists<*>?, position: Int) {
        itemId = when (shelf) {
            is Shelf.Lists.Categories -> shelf.list[position].id
            is Shelf.Lists.Items -> shelf.list[position].id
            is Shelf.Lists.Tracks -> shelf.list[position].id
            null -> null
        }

        val title = when (shelf) {
            is Shelf.Lists.Categories -> shelf.list[position].title
            is Shelf.Lists.Items -> shelf.list[position].title
            is Shelf.Lists.Tracks -> shelf.list[position].title
            null -> null
        }

        val subtitle = when (shelf) {
            is Shelf.Lists.Categories -> shelf.list[position].subtitle
            is Shelf.Lists.Items -> shelf.list[position].subtitle
            is Shelf.Lists.Tracks -> shelf.list[position].subtitle
            null -> null
        }

        binding.root.apply {
            setCardBackgroundColor(randomColor(itemId))
        }
        binding.title.text = title
        binding.subtitle.text = subtitle
        binding.subtitle.isVisible = !subtitle.isNullOrBlank()
        binding.root.isClickable = category?.items != null
    }

    override fun onCurrentChanged(current: PlayerState.Current?) {
        binding.root.alpha = if (current.isPlaying(itemId)) 0.5f else 1f
    }
}