package dev.brahmkshatriya.echo.ui.main.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.databinding.ItemQuickSearchMediaBinding
import dev.brahmkshatriya.echo.databinding.ItemQuickSearchQueryBinding
import dev.brahmkshatriya.echo.ui.feed.viewholders.MediaViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.ui.feed.viewholders.MediaViewHolder.Companion.subtitle
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadInto
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimViewHolder

sealed class QuickSearchViewHolder(itemView: View) : ScrollAnimViewHolder(itemView) {
    abstract fun bind(item: QuickSearchAdapter.Item)
    abstract val insertView: View
    abstract val deleteView: View
    open val transitionView: View
        get() = this.insertView

    class Query(val binding: ItemQuickSearchQueryBinding) : QuickSearchViewHolder(binding.root) {
        override val insertView: View
            get() = binding.insert

        override val deleteView: View
            get() = binding.delete

        override fun bind(item:  QuickSearchAdapter.Item) {
            val item = item.actual as QuickSearchItem.Query
            binding.history.visibility = if (item.searched) View.VISIBLE else View.INVISIBLE
            binding.query.text = item.query
        }

        companion object {
            fun create(
                parent: ViewGroup
            ): QuickSearchViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Query(
                    ItemQuickSearchQueryBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }

    class Media(val binding: ItemQuickSearchMediaBinding) : QuickSearchViewHolder(binding.root) {
        override val insertView: View
            get() = binding.insert

        override val deleteView: View
            get() = binding.delete

        override val transitionView: View
            get() = binding.coverContainer

        override fun bind(item:  QuickSearchAdapter.Item) {
            val item = item.actual as QuickSearchItem.Media
            binding.title.text = item.media.title
            val subtitle = item.media.subtitle(binding.root.context)
            binding.subtitle.text = subtitle
            binding.subtitle.isVisible = !subtitle.isNullOrEmpty()
            transitionView.transitionName = ("quick" + item.media.id).hashCode().toString()
            item.media.cover.loadInto(binding.cover, item.media.placeHolder)
        }

        companion object {
            fun create(
                parent: ViewGroup
            ): QuickSearchViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Media(
                    ItemQuickSearchMediaBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }
}