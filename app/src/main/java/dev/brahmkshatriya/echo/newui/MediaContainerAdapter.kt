package dev.brahmkshatriya.echo.newui

import android.os.Parcelable
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.newui.MediaContainerViewHolder.Category
import java.lang.ref.WeakReference

class MediaContainerAdapter(
    private val fragment: Fragment,
    val listener: MediaClickListener = MediaClickListener(fragment)
) : PagingDataAdapter<MediaItemsContainer, MediaContainerViewHolder>(
    DiffCallback
) {

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


    //Nested RecyclerView State Management

    init {
        addLoadStateListener {
            if (it.refresh == LoadState.Loading) clearState()
        }
    }

    private val stateViewModel: StateViewModel by fragment.viewModels()

    class StateViewModel : ViewModel() {
        val layoutManagerStates = hashMapOf<Int, Parcelable?>()
        val visibleScrollableViews = hashMapOf<Int, WeakReference<Category>>()
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

    override fun onViewRecycled(holder: MediaContainerViewHolder) {
        super.onViewRecycled(holder)
        if (holder is Category) saveScrollState(holder) {
            stateViewModel.visibleScrollableViews.remove(holder.bindingAdapterPosition)
        }
    }

    private fun saveScrollState(holder: Category, block: ((Category) -> Unit)? = null) {
        val layoutManagerStates = stateViewModel.layoutManagerStates
        layoutManagerStates[holder.bindingAdapterPosition] =
            holder.layoutManager?.onSaveInstanceState()
        block?.invoke(holder)
    }

    suspend fun submit(pagingData: PagingData<MediaItemsContainer>?) {
        saveState()
        submitData(pagingData ?: PagingData.empty())
    }

    // Binding

    override fun onBindViewHolder(holder: MediaContainerViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(item)
        val clickView = holder.clickView
        clickView.setOnClickListener {
            listener.onClick(item, holder.transitionView)
        }
        clickView.setOnLongClickListener {
            listener.onLongClick(item, holder.transitionView)
            true
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position) ?: return 0
        return when (item) {
            is MediaItemsContainer.Category -> 0
            is MediaItemsContainer.Item -> when (item.media) {
                is EchoMediaItem.TrackItem -> 1
                is EchoMediaItem.Profile -> 2
                is EchoMediaItem.Lists -> 3
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        0 -> Category.create(parent, stateViewModel, listener)

        else -> throw IllegalArgumentException("Invalid view type")
    }
}

