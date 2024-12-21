package dev.brahmkshatriya.echo.ui.adapter

import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.ui.adapter.ShelfLoadingAdapter.Companion.createListener
import dev.brahmkshatriya.echo.ui.adapter.ShelfViewHolder.Category
import dev.brahmkshatriya.echo.ui.adapter.ShelfViewHolder.Lists
import dev.brahmkshatriya.echo.ui.adapter.ShelfViewHolder.Media
import dev.brahmkshatriya.echo.ui.adapter.ShelfViewHolder.MediaLists
import dev.brahmkshatriya.echo.ui.adapter.ShelfViewHolder.MediaLists.ListViewModel
import dev.brahmkshatriya.echo.ui.editplaylist.SearchForPlaylistClickListener
import dev.brahmkshatriya.echo.ui.editplaylist.SearchForPlaylistFragment
import dev.brahmkshatriya.echo.ui.item.TrackAdapter
import dev.brahmkshatriya.echo.ui.paging.toFlow
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import kotlinx.coroutines.Job
import java.lang.ref.WeakReference

class ShelfAdapter(
    fragment: Fragment,
    private val transition: String,
    private val extension: Extension<*>,
    val listener: Listener = getListener(fragment)
) : PagingDataAdapter<Shelf, ShelfViewHolder>(DiffCallback) {

    fun applyCurrent(fragment: Fragment, recycler: RecyclerView): Job {
        val playerViewModel by fragment.activityViewModels<PlayerViewModel>()
        return fragment.observe(playerViewModel.currentFlow) {
            onCurrentChanged(recycler, it)
        }
    }

    interface Listener : TrackAdapter.Listener {
        fun onClick(clientId: String, shelf: Shelf, transitionView: View)
        fun onLongClick(clientId: String, shelf: Shelf, transitionView: View): Boolean
        fun onClick(clientId: String, item: EchoMediaItem, transitionView: View?)
        fun onLongClick(clientId: String, item: EchoMediaItem, transitionView: View?): Boolean
        fun onShuffleClick(clientId: String, shelf: Shelf.Lists.Tracks)
    }

    companion object {
        fun getListener(fragment: Fragment): Listener {
            val type = fragment.arguments?.getString("itemListener")
            return when (type) {
                "search" -> SearchForPlaylistClickListener(
                    if (fragment is SearchForPlaylistFragment) fragment.childFragmentManager
                    else fragment.parentFragmentManager
                )

                else -> ShelfClickListener(fragment.parentFragmentManager)
            }
        }
    }

    private val loadingListener = createListener(fragment) { retry() }
    private fun getLoadingAdapter() = ShelfLoadingAdapter(extension, loadingListener)

    fun withLoaders(): ConcatAdapter {
        val footer = getLoadingAdapter()
        val header = getLoadingAdapter()
        val empty = ShelfEmptyAdapter()
        addLoadStateListener { loadStates ->
            empty.loadState = if (loadStates.refresh is LoadState.NotLoading && itemCount == 0)
                LoadState.Loading
            else LoadState.NotLoading(false)
            header.loadState = loadStates.refresh
            footer.loadState = loadStates.append
        }
        return ConcatAdapter(empty, header, this, footer)
    }

    object DiffCallback : DiffUtil.ItemCallback<Shelf>() {
        override fun areItemsTheSame(
            oldItem: Shelf,
            newItem: Shelf
        ): Boolean {
            return oldItem.sameAs(newItem)
        }

        override fun areContentsTheSame(
            oldItem: Shelf,
            newItem: Shelf
        ): Boolean {
            return oldItem == newItem
        }
    }

    suspend fun submit(pagingData: PagingData<Shelf>?) {
        saveState()
        submitData(pagingData ?: PagingData.empty())
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
        val visibleScrollableViews = hashMapOf<Int, WeakReference<Lists>>()
    }

    private fun clearState() {
        stateViewModel.layoutManagerStates.clear()
        stateViewModel.visibleScrollableViews.clear()
    }

    private fun saveScrollState(holder: Lists, block: ((Lists) -> Unit)? = null) {
        val layoutManagerStates = stateViewModel.layoutManagerStates
        layoutManagerStates[holder.bindingAdapterPosition] =
            holder.layoutManager?.onSaveInstanceState()
        block?.invoke(holder)
    }

    private fun saveState() {
        stateViewModel.visibleScrollableViews.values.forEach { item ->
            item.get()?.let { saveScrollState(it) }
        }
        stateViewModel.visibleScrollableViews.clear()
    }

    override fun onViewRecycled(holder: ShelfViewHolder) {
        if (holder is Lists) saveScrollState(holder) {
            stateViewModel.visibleScrollableViews.remove(holder.bindingAdapterPosition)
        }
    }

    override fun onBindViewHolder(holder: ShelfViewHolder, position: Int) {
        val items = getItem(position) ?: return
        holder.transitionView.transitionName = (transition + items.id).hashCode().toString()
        holder.bind(items)
        holder.onCurrentChanged(current)
        val clickView = holder.clickView
        clickView.setOnClickListener {
            listener.onClick(extension.id, items, holder.transitionView)
        }
        clickView.setOnLongClickListener {
            listener.onLongClick(extension.id, items, holder.transitionView)
            true
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position) ?: return 0
        return when (item) {
            is Shelf.Lists<*> -> 0
            is Shelf.Item -> if (item.media is EchoMediaItem.Lists && item.loadTracks) 3 else 1
            is Shelf.Category -> 2
        }
    }

    private val sharedPool = RecycledViewPool()
    private val viewModel by fragment.activityViewModels<ListViewModel>()
    private val observe: (
        paged: PagedData.Single<Track>,
        block: suspend (PagingData<Track>) -> Unit
    ) -> Unit = { paged, block ->
        fragment.observe(paged.toFlow(), block)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShelfViewHolder {
        val holder = when (viewType) {
            0 -> Lists.create(parent, stateViewModel, sharedPool, extension.id, listener)
            1 -> Media.create(parent, extension.id, listener)
            2 -> Category.create(parent)
            3 -> MediaLists.create(parent, extension.id, listener, viewModel, observe)
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
        return holder
    }

    private var current: Current? = null
    private fun onCurrentChanged(recycler: RecyclerView, current: Current?) {
        this.current = current
        for (i in 0 until recycler.childCount) {
            val holder = recycler.getChildViewHolder(recycler.getChildAt(i)) as? ShelfViewHolder
            holder?.onCurrentChanged(current)
        }
    }
}