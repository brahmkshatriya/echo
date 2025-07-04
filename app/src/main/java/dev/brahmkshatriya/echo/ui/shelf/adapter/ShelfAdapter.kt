package dev.brahmkshatriya.echo.ui.shelf.adapter

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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.SkeletonShelfBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.common.PagingUtils
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.shelf.adapter.lists.ShelfListsAdapter
import dev.brahmkshatriya.echo.ui.shelf.adapter.other.ShelfEmptyAdapter
import dev.brahmkshatriya.echo.ui.shelf.adapter.other.ShelfLoadingAdapter
import dev.brahmkshatriya.echo.ui.shelf.adapter.other.ShelfLoadingAdapter.Companion.createListener
import dev.brahmkshatriya.echo.ui.shelf.adapter.other.ShelfSearchHeaderAdapter
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.lang.ref.WeakReference

class ShelfAdapter(
    private val listener: Listener, private val stateViewModel: StateViewModel
) : PagingDataAdapter<Shelf, ShelfAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Shelf>() {
        override fun areItemsTheSame(oldItem: Shelf, newItem: Shelf) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Shelf, newItem: Shelf) = oldItem == newItem
    }

    abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var extensionId: String? = null
        open var scrollAmount: Int = 0
        open fun onCurrentChanged(current: PlayerState.Current?) {}
        abstract fun bind(item: Shelf?)
    }

    interface Listener : ShelfListsAdapter.Listener {
        fun onMoreClicked(extensionId: String?, shelf: Shelf.Lists<*>?, view: View)
        fun onShuffleClicked(extensionId: String?, shelf: Shelf.Lists.Tracks?, view: View)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.scrollAmount = scrollY
        holder.bind(getItemOrNull(position))
        holder.onCurrentChanged(current)
        if (holder !is ListsShelfViewHolder) return
        stateViewModel.visibleScrollableViews[position] = WeakReference(holder)
        holder.layoutManager?.apply {
            val state = stateViewModel.layoutManagerStates[position]
            if (state != null) onRestoreInstanceState(state)
            else scrollToPosition(0)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val (type, extra) = when (val item = getItemOrNull(position)) {
            is Shelf.Item -> 1 to MediaItemViewHolder.getViewType(item)
            is Shelf.Lists<*> -> 3 to null
            else -> 2 to null
        }
        return type * 10 + (extra ?: 0)
    }

    private val sharedPool = RecycledViewPool()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val (type, extra) = viewType / 10 to viewType % 10
        val viewHolder = when (type) {
            1 -> MediaItemViewHolder.create(listener, this, inflater, parent, extra)
            2 -> CategoryShelfViewHolder.create(listener, inflater, parent)
            3 -> ListsShelfViewHolder.create(sharedPool, listener, inflater, parent)
            else -> error("unknown view type")
        }
        viewHolder.extensionId = extensionId
        return viewHolder
    }

    var current: PlayerState.Current? = null
    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.onCurrentChanged(current)
        holder.extensionId = extensionId
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.onCurrentChanged(current)
        holder.extensionId = extensionId
    }

    private var scrollY: Int = 0
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            scrollY = dy
            onEachViewHolder { scrollAmount = dy }
        }
    }

    var recyclerView: RecyclerView? = null
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        recyclerView.addOnScrollListener(scrollListener)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        recyclerView.removeOnScrollListener(scrollListener)
        this.recyclerView = null
    }

    private fun onEachViewHolder(action: ViewHolder.() -> Unit) {
        recyclerView?.let { rv ->
            for (i in 0 until rv.childCount) {
                val holder = rv.getChildViewHolder(rv.getChildAt(i)) as? ViewHolder
                holder?.action()
            }
        }
    }

    fun onCurrentChanged(current: PlayerState.Current?) {
        this.current = current
        onEachViewHolder { onCurrentChanged(current) }
    }

    init {
        addLoadStateListener {
            if (it.refresh == LoadState.Loading) clearState()
        }
    }

    var extensionId: String? = null
    var shelf: PagedData<Shelf>? = null
    suspend fun submit(
        extensionId: String?,
        data: PagedData<Shelf>?,
        pagingData: PagingData<Shelf>?
    ) {
        saveState()
        this.extensionId = extensionId
        onEachViewHolder { this.extensionId = extensionId }
        this.shelf = data
        val page = if (extensionId == null) PagingUtils.loadingPagingData()
        else pagingData ?: PagingData.empty()
        submitData(page)
    }

    class StateViewModel : ViewModel() {
        val layoutManagerStates = hashMapOf<Int, Parcelable?>()
        val visibleScrollableViews = hashMapOf<Int, WeakReference<ListsShelfViewHolder>>()
    }

    private fun clearState() {
        stateViewModel.layoutManagerStates.clear()
        stateViewModel.visibleScrollableViews.clear()
    }

    private fun saveScrollState(
        holder: ListsShelfViewHolder, block: ((ListsShelfViewHolder) -> Unit)? = null
    ) {
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

    override fun onViewRecycled(holder: ViewHolder) {
        if (holder is ListsShelfViewHolder) saveScrollState(holder) {
            stateViewModel.visibleScrollableViews.remove(holder.bindingAdapterPosition)
        }
    }

    fun getTracks() = snapshot().items.filterIsInstance<Shelf.Item>()
        .mapNotNull { (it.media as? EchoMediaItem.TrackItem)?.track }

    data class Loading(
        val inflater: LayoutInflater,
        val parent: ViewGroup,
        val binding: SkeletonShelfBinding =
            SkeletonShelfBinding.inflate(inflater, parent, false)
    ) : ShelfLoadingAdapter.ViewHolder(binding.root)

    fun withLoaders(
        fragment: Fragment,
        vararg adapters: RecyclerView.Adapter<*>,
    ): ConcatAdapter {
        val listener = fragment.createListener { retry() }
        val footer = ShelfLoadingAdapter(::Loading, listener)
        val header = ShelfLoadingAdapter(::Loading, listener)
        val empty = ShelfEmptyAdapter()
        addLoadStateListener { loadStates ->
            empty.loadState =
                if (loadStates.refresh is LoadState.NotLoading && itemCount == 0) LoadState.Loading
                else LoadState.NotLoading(false)
            header.loadState = loadStates.refresh
            footer.loadState = loadStates.append
        }
        return ConcatAdapter(*adapters, empty, header, this, footer)
    }

    fun withHeaders(
        fragment: Fragment,
        viewModel: ViewModel,
        stateFlow: MutableStateFlow<PagingUtils.Data<Shelf>>,
        shelfJob: MutableStateFlow<Job?>
    ): ConcatAdapter {
        val header = ShelfSearchHeaderAdapter(fragment, viewModel, stateFlow, shelfJob)
        addOnPagesUpdatedListener {
            val visible = shelf != null
            header.submit(visible)
        }
        return withLoaders(fragment, header)
    }

    private fun getItemOrNull(i: Int) = if (i in 0 until itemCount) getItem(i) else null
    fun getTouchHelper(): ItemTouchHelper {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START) {
            override fun getMovementFlags(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder
            ): Int {
                val mediaItem = getItemOrNull(viewHolder.bindingAdapterPosition) as? Shelf.Item
                    ?: return 0
                return if (mediaItem.media !is EchoMediaItem.TrackItem) 0
                else makeMovementFlags(0, ItemTouchHelper.START)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val mediaItem = getItemOrNull(viewHolder.bindingAdapterPosition) as? Shelf.Item
                    ?: return
                val track = (mediaItem.media as? EchoMediaItem.TrackItem)?.track ?: return
                listener.onTrackSwiped(extensionId, listOf(track), 0, null, viewHolder.itemView)
                notifyItemChanged(viewHolder.bindingAdapterPosition)
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder) = 0.25f
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false
        }
        return ItemTouchHelper(callback)
    }

    companion object {
        fun Fragment.getShelfAdapter(
            listener: Listener
        ): ShelfAdapter {
            val viewModel by activityViewModel<PlayerViewModel>()
            val stateViewModel by viewModels<StateViewModel>()
            val adapter = ShelfAdapter(listener, stateViewModel)
            observe(viewModel.playerState.current) { adapter.onCurrentChanged(it) }
            return adapter
        }
    }
}