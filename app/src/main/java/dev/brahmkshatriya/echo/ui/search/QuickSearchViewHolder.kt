package dev.brahmkshatriya.echo.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.QuickSearch
import dev.brahmkshatriya.echo.databinding.ItemQuickSearchMediaBinding
import dev.brahmkshatriya.echo.databinding.ItemQuickSearchQueryBinding
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.utils.loadInto

sealed class QuickSearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: QuickSearch)
    abstract val insertView: View
    open val transitionView: View
        get() = this.insertView

    class Query(val binding: ItemQuickSearchQueryBinding) : QuickSearchViewHolder(binding.root) {
        override val insertView: View
            get() = binding.insert

        override fun bind(item: QuickSearch) {
            item as QuickSearch.QueryItem
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

        override val transitionView: View
            get() = binding.coverContainer

        override fun bind(item: QuickSearch) {
            item as QuickSearch.MediaItem
            binding.query.text = item.media.title
            transitionView.transitionName = ("quick" + item.media.id).hashCode().toString()
            item.media.cover.loadInto(binding.cover, item.media.placeHolder())
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