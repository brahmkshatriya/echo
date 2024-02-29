package dev.brahmkshatriya.echo.ui.adapters

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.databinding.ItemCategoryBinding
import dev.brahmkshatriya.echo.databinding.ItemTrackBinding
import dev.brahmkshatriya.echo.player.PlayerHelper.Companion.toTimeString
import dev.brahmkshatriya.echo.ui.ClickListener
import dev.brahmkshatriya.echo.ui.MediaItemClickListener
import dev.brahmkshatriya.echo.utils.loadInto
import java.lang.ref.WeakReference

class MediaItemsContainerAdapter(
    fragment: Fragment,
    private val listener: ClickListener<Pair<View, EchoMediaItem>> = MediaItemClickListener(fragment),
) : PagingDataAdapter<MediaItemsContainer, MediaItemsContainerAdapter.MediaItemsContainerHolder>(
    MediaItemsContainerComparator
) {

    private val lifecycle = fragment.lifecycle

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
        0 -> Category(
            ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        else -> Track(
            ItemTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }


    override fun onBindViewHolder(holder: MediaItemsContainerHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind()

        val echoMediaItem = when (item) {
            is MediaItemsContainer.TrackItem -> holder.transitionView to item.track.toMediaItem()
            else -> null
        }
        echoMediaItem?.let {
            holder.itemView.apply {
                setOnClickListener {
                    listener.onClick(echoMediaItem)
                }
                setOnLongClickListener {
                    listener.onLongClick(echoMediaItem)
                    true
                }
            }
        }
    }

    //NESTED RECYCLER VIEW THINGS

    private val stateViewModel: StateViewModel by fragment.viewModels()

    class StateViewModel : ViewModel() {
        val layoutManagerStates = hashMapOf<Int, Parcelable?>()
        val visibleScrollableViews = hashMapOf<Int, WeakReference<Category>>()
    }

    suspend fun submit(pagingData: PagingData<MediaItemsContainer>) {
        saveState()
        submitData(pagingData)
    }

    init {
        addLoadStateListener {
            if (it.refresh == LoadState.Loading) clearState()
        }
    }

    private fun clearState() {
        stateViewModel.layoutManagerStates.clear()
        stateViewModel.visibleScrollableViews.clear()
    }

    private fun saveState() {
        stateViewModel.visibleScrollableViews.values.forEach { item ->
            item.get()?.let { saveScrollState(it) }
        }
        stateViewModel.visibleScrollableViews.clear()
    }

    override fun onViewRecycled(holder: MediaItemsContainerHolder) {
        super.onViewRecycled(holder)
        if (holder is Category) saveScrollState(holder) {
            stateViewModel.visibleScrollableViews.remove(holder.bindingAdapterPosition)
        }
    }

    private fun saveScrollState(holder: Category, block: ((Category) -> Unit)? = null) {
        val layoutManagerStates = stateViewModel.layoutManagerStates
        layoutManagerStates[holder.bindingAdapterPosition] =
            holder.layoutManager.onSaveInstanceState()
        block?.invoke(holder)
    }

    // VIEW HOLDER

    abstract inner class MediaItemsContainerHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        abstract fun bind()
        abstract val transitionView: View
    }

    inner class Category(val binding: ItemCategoryBinding) :
        MediaItemsContainerHolder(binding.root) {
        private val adapter = MediaItemAdapter(listener)
        val layoutManager = LinearLayoutManager(binding.root.context, HORIZONTAL, false)
        override fun bind() {
            val item = getItem(bindingAdapterPosition) ?: return
            val position = bindingAdapterPosition
            val category = item as MediaItemsContainer.Category
            binding.textView.text = category.title
            binding.recyclerView.layoutManager = layoutManager
            binding.recyclerView.adapter = adapter
            adapter.submitData(lifecycle, category.list)
            layoutManager.let {
                val state: Parcelable? = stateViewModel.layoutManagerStates[position]
                if (state != null) it.onRestoreInstanceState(state)
                else it.scrollToPosition(0)
            }
            stateViewModel.visibleScrollableViews[position] = WeakReference(this)
        }

        override val transitionView
            get() = throw IllegalArgumentException()
    }

    inner class Track(val binding: ItemTrackBinding) : MediaItemsContainerHolder(binding.root) {
        override fun bind() {
            val item = getItem(bindingAdapterPosition) ?: return
            val track = (item as MediaItemsContainer.TrackItem).track
            binding.title.text = track.title
            track.cover.loadInto(binding.imageView, R.drawable.art_music)
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

        override val transitionView = binding.root
    }

    companion object MediaItemsContainerComparator : DiffUtil.ItemCallback<MediaItemsContainer>() {

        override fun areItemsTheSame(
            oldItem: MediaItemsContainer, newItem: MediaItemsContainer
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
            oldItem: MediaItemsContainer, newItem: MediaItemsContainer
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