package dev.brahmkshatriya.echo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.data.models.MediaItemsContainer
import dev.brahmkshatriya.echo.data.models.Track
import dev.brahmkshatriya.echo.databinding.ItemMediaRecyclerBinding

class MediaItemsContainerAdapter(
    private val lifecycle: Lifecycle,
    private val play: (Track) -> Unit,
) :
    PagingDataAdapter<MediaItemsContainer, MediaItemsContainerAdapter.MediaItemsContainerHolder>(
        MediaItemsContainerComparator
    ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MediaItemsContainerHolder(
        ItemMediaRecyclerBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: MediaItemsContainerHolder, position: Int) {
        val item = getItem(position) ?: return
        val binding = holder.binding
        binding.textView.text = item.title
        binding.recyclerView.layoutManager =
            LinearLayoutManager(binding.root.context, HORIZONTAL, false)
        val adapter = MediaItemAdapter(play)
        binding.recyclerView.adapter = adapter
        adapter.submitData(lifecycle, item.list)
    }

    class MediaItemsContainerHolder(val binding: ItemMediaRecyclerBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object MediaItemsContainerComparator : DiffUtil.ItemCallback<MediaItemsContainer>() {

        override fun areItemsTheSame(
            oldItem: MediaItemsContainer,
            newItem: MediaItemsContainer
        ) = oldItem.title == newItem.title

        override fun areContentsTheSame(
            oldItem: MediaItemsContainer,
            newItem: MediaItemsContainer
        ) = oldItem == newItem
    }
}