package dev.brahmkshatriya.echo.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.data.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.data.models.Track
import dev.brahmkshatriya.echo.databinding.ItemCategoryBinding
import dev.brahmkshatriya.echo.databinding.ItemTrackBinding
import dev.brahmkshatriya.echo.player.PlayerHelper.Companion.toTimeString
import dev.brahmkshatriya.echo.ui.utils.loadInto

class MediaItemsContainerAdapter(
    private val lifecycle: Lifecycle,
    private val listener: ClickListener<Track>,
) : PagingDataAdapter<MediaItemsContainer, MediaItemsContainerAdapter.MediaItemsContainerHolder>(
    MediaItemsContainerComparator
) {

    fun withLoadingFooter(): ConcatAdapter {
        val footer = ContainerLoadingAdapter {
            retry()
        }
        addLoadStateListener { loadStates ->
            footer.loadState = when (loadStates.refresh) {
                is LoadState.NotLoading -> loadStates.append
                else -> loadStates.refresh
            }
        }
        return ConcatAdapter(this, footer)
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.let {
            when (it) {
                is MediaItemsContainer.Category -> 0
                is MediaItemsContainer.TrackItem -> 1
            }
        } ?: 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        0 -> MediaItemsContainerHolder(
            MediaItemsContainerBinding.Category(
                ItemCategoryBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
            )
        )

        else -> MediaItemsContainerHolder(
            MediaItemsContainerBinding.Track(
                ItemTrackBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
            )
        )
    }


    override fun onBindViewHolder(holder: MediaItemsContainerHolder, position: Int) {
        val item = getItem(position) ?: return
        when (holder.container) {
            is MediaItemsContainerBinding.Category -> {
                val binding = holder.container.binding
                val category = item as MediaItemsContainer.Category
                binding.textView.text = category.title
                binding.recyclerView.layoutManager =
                    LinearLayoutManager(binding.root.context, HORIZONTAL, false)
                val adapter = MediaItemAdapter(listener)
                binding.recyclerView.adapter = adapter
                adapter.submitData(lifecycle, category.list)
            }

            is MediaItemsContainerBinding.Track -> {
                val binding = holder.container.binding
                val track = (item as MediaItemsContainer.TrackItem).track
                binding.root.setOnClickListener { listener.onClick(track) }
                binding.root.setOnLongClickListener {
                    listener.onLongClick(track)
                    true
                }

                binding.title.text = track.title

                track.cover?.loadInto(binding.imageView)

                val album = track.album
                if (album == null) {
                    binding.album.visibility = View.GONE
                } else {
                    binding.album.visibility = View.VISIBLE
                    binding.album.text = album.title
                }

                if (track.artists.isEmpty()) {
                    binding.artist.visibility = View.GONE
                } else {
                    binding.artist.visibility = View.VISIBLE
                    binding.artist.text = track.artists.joinToString(" ") { it.name }
                }

                val duration = track.duration
                if (duration == null) {
                    binding.duration.visibility = View.GONE
                } else {
                    binding.duration.visibility = View.VISIBLE
                    binding.duration.text = duration.toTimeString()
                }
            }
        }

    }

    sealed class MediaItemsContainerBinding {
        data class Category(val binding: ItemCategoryBinding) : MediaItemsContainerBinding()
        data class Track(val binding: ItemTrackBinding) : MediaItemsContainerBinding()
    }

    class MediaItemsContainerHolder(val container: MediaItemsContainerBinding) :
        RecyclerView.ViewHolder(
            when (container) {
                is MediaItemsContainerBinding.Category -> container.binding.root
                is MediaItemsContainerBinding.Track -> container.binding.root
            }
        )

    companion object MediaItemsContainerComparator : DiffUtil.ItemCallback<MediaItemsContainer>() {

        override fun areItemsTheSame(
            oldItem: MediaItemsContainer,
            newItem: MediaItemsContainer
        ): Boolean {
            return when (oldItem) {
                is MediaItemsContainer.Category -> {
                    val newCategory = newItem as? MediaItemsContainer.Category
                    oldItem.title == newCategory?.title
                }

                is MediaItemsContainer.TrackItem -> {
                    val newTrack = newItem as? MediaItemsContainer.TrackItem
                    oldItem.track.uri == newTrack?.track?.uri
                }
            }
        }

        override fun areContentsTheSame(
            oldItem: MediaItemsContainer,
            newItem: MediaItemsContainer
        ): Boolean {
            when (oldItem) {
                is MediaItemsContainer.Category -> {
                    val newCategory = newItem as? MediaItemsContainer.Category
                    newCategory ?: return true
                    oldItem.list.forEachIndexed { index, mediaItem ->
                        if (newCategory.list.getOrNull(index) != mediaItem) return false
                    }
                }

                is MediaItemsContainer.TrackItem -> {
                    val newTrack = newItem as? MediaItemsContainer.TrackItem
                    return oldItem.track == newTrack?.track
                }
            }
            return true
        }
    }

    class ListCallback : ListUpdateCallback {
        override fun onChanged(position: Int, count: Int, payload: Any?) {}
        override fun onMoved(fromPosition: Int, toPosition: Int) {}
        override fun onInserted(position: Int, count: Int) {}
        override fun onRemoved(position: Int, count: Int) {}
    }
}