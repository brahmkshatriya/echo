package dev.brahmkshatriya.echo.ui.shelf.adapter.other

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.ItemShelfEmptyBinding

class ShelfEmptyAdapter : LoadStateAdapter<ShelfEmptyAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemShelfEmptyBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemShelfEmptyBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, loadState: LoadState) {}
}