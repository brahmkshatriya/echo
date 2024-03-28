package dev.brahmkshatriya.echo.newui

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer

class MediaContainerAdapter(
    private val fragment: Fragment
) : PagingDataAdapter<MediaItemsContainer, MediaContainerViewHolder>(
    DiffCallback
) {

    override fun onBindViewHolder(holder: MediaContainerViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(item)
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position) ?: return 0
        return when (item) {
            is MediaItemsContainer.Category -> 0
            is MediaItemsContainer.TrackItem -> 1
            is MediaItemsContainer.AlbumItem -> 2
            is MediaItemsContainer.ArtistItem -> 3
            is MediaItemsContainer.PlaylistItem -> 4
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        0 -> MediaContainerViewHolder.Category.create(parent)
        else -> throw IllegalArgumentException("Invalid view type")
    }

    suspend fun submit(pagingData: PagingData<MediaItemsContainer>?) {
        submitData(pagingData ?: PagingData.empty())
    }

    fun withLoaders(): ConcatAdapter {
        val footer = ContainerLoadingAdapter(fragment) { retry() }
        val header = ContainerLoadingAdapter(fragment) { retry() }
        addLoadStateListener { loadStates ->
            header.loadState = loadStates.refresh
            footer.loadState = loadStates.append
        }
        return ConcatAdapter(header, this, footer)
    }

    object DiffCallback : DiffUtil.ItemCallback<MediaItemsContainer>() {
        override fun areItemsTheSame(
            oldItem: MediaItemsContainer,
            newItem: MediaItemsContainer
        ): Boolean {
            return oldItem.sameAs(newItem)
        }

        override fun areContentsTheSame(
            oldItem: MediaItemsContainer,
            newItem: MediaItemsContainer
        ): Boolean {
            return oldItem == newItem
        }
    }
}

