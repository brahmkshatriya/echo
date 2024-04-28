package dev.brahmkshatriya.echo.ui.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.ItemNothingToShowBinding

class MediaContainerEmptyAdapter : LoadStateAdapter<MediaContainerEmptyAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemNothingToShowBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemNothingToShowBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, loadState: LoadState) {}
}