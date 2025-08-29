package dev.brahmkshatriya.echo.ui.feed.viewholders

import android.graphics.Color
import android.graphics.Color.HSVToColor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfCategoryBinding
import dev.brahmkshatriya.echo.ui.feed.FeedClickListener
import dev.brahmkshatriya.echo.ui.feed.FeedType
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadInto
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isNightMode
import kotlin.math.roundToInt

class CategoryViewHolder(
    parent: ViewGroup,
    listener: FeedClickListener,
    private val binding: ItemShelfCategoryBinding = ItemShelfCategoryBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
    )
) : FeedViewHolder<FeedType.Category>(binding.root) {

    private var feed: FeedType.Category? = null

    init {
        binding.root.setOnClickListener {
            listener.openFeed(
                it,
                feed?.extensionId,
                feed?.id,
                feed?.shelf?.title,
                feed?.shelf?.subtitle,
                feed?.shelf?.feed
            )
        }
    }

    override fun bind(feed: FeedType.Category) {
        this.feed = feed
        val category = feed.shelf
        binding.bind(category)
    }

    companion object {
        fun ItemShelfCategoryBinding.bind(category: Shelf.Category) {
            title.text = category.title
            subtitle.text = category.subtitle
            subtitle.isVisible = !category.subtitle.isNullOrEmpty()
            icon.isVisible = category.image != null
            category.image.loadInto(icon)
            root.run {
                val color = applyBackground(category.backgroundColor)
                    ?: ResourcesCompat.getColor(resources, R.color.amoled_fg_semi, null)
                setCardBackgroundColor(color)
            }
        }

        fun CardView.applyBackground(hex: String?): Int? {
            val hsv = runCatching { hex?.toColorInt() }.getOrNull()?.run {
                val hsv = FloatArray(3)
                Color.colorToHSV(this, hsv)
                hsv
            } ?: return null
            val actualSat = (hsv[1] * 0.25).roundToInt()
            val sat = if (context.isNightMode()) (35f + actualSat) / 100 else 0.2f
            val value = if (context.isNightMode()) 0.5f else 0.9f
            val color = HSVToColor(floatArrayOf(hsv[0], sat, value))
            val with = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary)
            return MaterialColors.harmonize(color, with)
        }
    }
}