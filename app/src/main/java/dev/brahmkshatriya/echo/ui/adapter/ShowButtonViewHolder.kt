package dev.brahmkshatriya.echo.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfShowButtonBinding

class ShowButtonViewHolder(
    val binding: ItemShelfShowButtonBinding
) : ShelfListItemViewHolder(binding.root) {

    @SuppressLint("NotifyDataSetChanged")
    override fun bind(item: Any) {
        item as Boolean
        val id = if (item) R.string.show_less else R.string.show_all
        binding.show.setText(id)
        binding.show.setOnClickListener { onClick(item, shelf.list) }
    }

    private fun onClick(showingAll: Boolean, list: List<Any>) {
        if (showingAll) adapter.submitList(list.take(MAX) + false)
        else adapter.submitList(list + true)
    }

    override val transitionView = binding.root

    companion object {

        private const val MAX = 3
        fun create(parent: ViewGroup): ShowButtonViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ShowButtonViewHolder(
                ItemShelfShowButtonBinding.inflate(inflater, parent, false)
            )
        }

        fun initialize(adapter: ShelfListItemViewAdapter, shelf: Shelf.Lists.Tracks) {
            if (shelf.list.size <= MAX) adapter.submitList(shelf.list)
            else {
                val list = shelf.list.subList(0, MAX)
                adapter.submitList(list + false)
            }
        }
    }
}