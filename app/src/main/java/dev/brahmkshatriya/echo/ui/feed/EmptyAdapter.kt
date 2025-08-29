package dev.brahmkshatriya.echo.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.LoadState
import dev.brahmkshatriya.echo.databinding.ItemShelfEmptyBinding
import dev.brahmkshatriya.echo.ui.common.GridAdapter
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimLoadStateAdapter
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimViewHolder

class EmptyAdapter : ScrollAnimLoadStateAdapter<EmptyAdapter.ViewHolder>(), GridAdapter {
    class ViewHolder(val binding: ItemShelfEmptyBinding) : ScrollAnimViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemShelfEmptyBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override val adapter = this
    override fun getSpanSize(position: Int, width: Int, count: Int) = count
}