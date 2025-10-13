package dev.brahmkshatriya.echo.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.paging.LoadState
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemLoadingBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.common.GridAdapter
import dev.brahmkshatriya.echo.ui.feed.FeedData.FeedTab
import dev.brahmkshatriya.echo.ui.feed.FeedLoadingAdapter.Companion.createListener
import dev.brahmkshatriya.echo.ui.feed.FeedType.Enum.Category
import dev.brahmkshatriya.echo.ui.feed.FeedType.Enum.CategoryGrid
import dev.brahmkshatriya.echo.ui.feed.FeedType.Enum.Header
import dev.brahmkshatriya.echo.ui.feed.FeedType.Enum.HorizontalList
import dev.brahmkshatriya.echo.ui.feed.FeedType.Enum.Media
import dev.brahmkshatriya.echo.ui.feed.FeedType.Enum.MediaGrid
import dev.brahmkshatriya.echo.ui.feed.FeedType.Enum.Video
import dev.brahmkshatriya.echo.ui.feed.FeedType.Enum.VideoHorizontal
import dev.brahmkshatriya.echo.ui.feed.viewholders.CategoryViewHolder
import dev.brahmkshatriya.echo.ui.feed.viewholders.FeedViewHolder
import dev.brahmkshatriya.echo.ui.feed.viewholders.HeaderViewHolder
import dev.brahmkshatriya.echo.ui.feed.viewholders.HorizontalListViewHolder
import dev.brahmkshatriya.echo.ui.feed.viewholders.MediaGridViewHolder
import dev.brahmkshatriya.echo.ui.feed.viewholders.MediaViewHolder
import dev.brahmkshatriya.echo.ui.feed.viewholders.VideoHorizontalViewHolder
import dev.brahmkshatriya.echo.ui.feed.viewholders.VideoViewHolder
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.animatedWithAlpha
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimPagingAdapter
import kotlinx.coroutines.flow.combine
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.lang.ref.WeakReference

class FeedAdapter(
    private val viewModel: FeedData,
    private val listener: FeedClickListener,
    private val takeFullScreen: Boolean = false,
) : ScrollAnimPagingAdapter<FeedType, FeedViewHolder<*>>(DiffCallback), GridAdapter {

    object DiffCallback : DiffUtil.ItemCallback<FeedType>() {
        override fun areContentsTheSame(oldItem: FeedType, newItem: FeedType) = oldItem == newItem
        override fun areItemsTheSame(oldItem: FeedType, newItem: FeedType): Boolean {
            if (oldItem.extensionId != newItem.extensionId) return false
            if (newItem.type != oldItem.type) return false
            if (oldItem.id != newItem.id) return false
            return true
        }
    }

    private val viewPool = RecyclerView.RecycledViewPool()
    override fun getItemViewType(position: Int) =
        runCatching { getItem(position)!! }.getOrNull()?.type?.ordinal ?: 0

    private var isPlayButtonShown = false
    private fun FeedType.toTrack(): Track? = when (this) {
        is FeedType.Media -> item as? Track
        is FeedType.MediaGrid -> item as? Track
        is FeedType.Video -> item
        else -> null
    }

    fun getAllTracks(feed: FeedType): Pair<List<Track>, Int> {
        if (!isPlayButtonShown) return listOfNotNull(feed.toTrack()) to 0
        val list = snapshot().mapNotNull { it }
        val index = list.indexOfFirst { it.id == feed.id }
        if (index == -1) return listOf<Track>() to -1
        val from = list.take(index).indexOfLast { it.type != feed.type }
        val to = list.drop(index + 1).indexOfFirst { it.type != feed.type }
        val feeds = list.subList(from + 1, if (to == -1) list.size else index + to + 1)
        val tracks = feeds.mapNotNull { it.toTrack() }
        val newIndex = tracks.indexOfFirst { it.id == feed.id }
        return tracks to newIndex
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder<*> {
        val type = FeedType.Enum.entries[viewType]
        return when (type) {
            Header -> HeaderViewHolder(parent, listener)
            HorizontalList -> HorizontalListViewHolder(parent, listener, viewPool)
            Category -> CategoryViewHolder(parent, listener)
            CategoryGrid -> CategoryViewHolder(parent, listener)
            Media -> MediaViewHolder(parent, listener, ::getAllTracks)
            MediaGrid -> MediaGridViewHolder(parent, listener, ::getAllTracks)
            Video -> VideoViewHolder(parent, listener, ::getAllTracks)
            VideoHorizontal -> VideoHorizontalViewHolder(parent, listener, ::getAllTracks)
        }
    }

    override fun onBindViewHolder(holder: FeedViewHolder<*>, position: Int) {
        super.onBindViewHolder(holder, position)
        val feed = runCatching { getItem(position) }.getOrNull() ?: return
        when (holder) {
            is HeaderViewHolder -> holder.bind(feed as FeedType.Header)
            is CategoryViewHolder -> holder.bind(feed as FeedType.Category)
            is MediaViewHolder -> holder.bind(feed as FeedType.Media)
            is MediaGridViewHolder -> holder.bind(feed as FeedType.MediaGrid)
            is VideoViewHolder -> holder.bind(feed as FeedType.Video)
            is VideoHorizontalViewHolder -> holder.bind(feed as FeedType.Video)
            is HorizontalListViewHolder -> {
                holder.bind(feed as FeedType.HorizontalList)
                viewModel.visibleScrollableViews[position] = WeakReference(holder)
                holder.layoutManager.apply {
                    val state = viewModel.layoutManagerStates[position]
                    if (state != null) onRestoreInstanceState(state)
                    else scrollToPosition(0)
                }
            }
        }
        holder.onCurrentChanged(current)
    }

    override fun onViewRecycled(holder: FeedViewHolder<*>) {
        if (holder is HorizontalListViewHolder) saveScrollState(holder) {
            viewModel.visibleScrollableViews.remove(holder.bindingAdapterPosition)
        }
    }

    override fun onViewAttachedToWindow(holder: FeedViewHolder<*>) {
        holder.onCurrentChanged(current)
    }

    class LoadingViewHolder(
        parent: ViewGroup,
        val binding: ItemLoadingBinding = ItemLoadingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ),
    ) : FeedLoadingAdapter.ViewHolder(binding.root) {
        init {
            binding.textView.isVisible = false
        }

        override fun bind(loadState: LoadState) {
            binding.root.alpha = 0f
            binding.root.animatedWithAlpha(500)
        }
    }

    fun getAllTracks() = snapshot().mapNotNull {
        when (it) {
            is FeedType.Media -> listOfNotNull(it.item as? Track)
            is FeedType.MediaGrid -> listOfNotNull(it.item as? Track)
            is FeedType.Video -> listOf(it.item)
            is FeedType.HorizontalList -> it.shelf.list.filterIsInstance<Track>()
            else -> null
        }
    }.flatten()

    fun withLoading(fragment: Fragment, vararg before: GridAdapter): GridAdapter.Concat {
        val tabs = TabsAdapter<FeedTab>({ tab.title }) { view, index, tab ->
            listener.onTabSelected(view, tab.feedId, tab.extensionId, index)
        }
        fragment.observe(viewModel.tabsFlow) { tabs.data = it }
        fragment.observe(viewModel.selectedTabIndexFlow) { tabs.selected = it }
        val buttons = ButtonsAdapter(viewModel, listener, ::getAllTracks)
        fragment.observe(viewModel.buttonsFlow) {
            buttons.buttons = it
            isPlayButtonShown = it?.buttons?.showPlayAndShuffle == true
        }
        val loadStateListener = fragment.createListener { retry() }
        val header = FeedLoadingAdapter(loadStateListener) { LoadingViewHolder(it) }
        val footer = FeedLoadingAdapter(loadStateListener) { LoadingViewHolder(it) }
        val empty = EmptyAdapter()
        fragment.observe(
            loadStateFlow.combine(viewModel.shouldShowEmpty) { a, b -> a to b }
        ) { (loadStates, shouldShowEmpty) ->
            val isEmpty =
                shouldShowEmpty && itemCount == 0 && loadStates.append is LoadState.NotLoading
            empty.loadState = if (isEmpty) LoadState.Loading else LoadState.NotLoading(false)
        }
        addLoadStateListener { loadStates ->
            header.loadState = loadStates.refresh
            footer.loadState = loadStates.append
        }
        return GridAdapter.Concat(*before, tabs, buttons, header, empty, this, footer)
    }

    override val adapter = this
    override fun getSpanSize(position: Int, width: Int, count: Int) =
        when (FeedType.Enum.entries[getItemViewType(position)]) {
            Header, HorizontalList -> count
            Category, Media, Video -> if (takeFullScreen) count else 2.coerceAtMost(count)
            CategoryGrid, MediaGrid, VideoHorizontal -> 1
        }

    private fun clearState() {
        viewModel.layoutManagerStates.clear()
        viewModel.visibleScrollableViews.clear()
    }

    private fun saveScrollState(
        holder: HorizontalListViewHolder, block: ((HorizontalListViewHolder) -> Unit)? = null,
    ) = runCatching {
        val layoutManagerStates = viewModel.layoutManagerStates
        layoutManagerStates[holder.bindingAdapterPosition] =
            holder.layoutManager.onSaveInstanceState()
        block?.invoke(holder)
    }

    fun saveState() {
        viewModel.visibleScrollableViews.values.forEach { item ->
            item.get()?.let { saveScrollState(it) }
        }
        viewModel.visibleScrollableViews.clear()
    }

    init {
        addLoadStateListener {
            if (it.refresh == LoadState.Loading) clearState()
        }
    }

    private var current: PlayerState.Current? = null
    fun onCurrentChanged(current: PlayerState.Current?) {
        this.current = current
        onEachViewHolder { onCurrentChanged(current) }
    }

    companion object {
        fun Fragment.getFeedAdapter(
            viewModel: FeedData,
            listener: FeedClickListener,
            takeFullScreen: Boolean = false,
        ): FeedAdapter {
            val playerViewModel by activityViewModel<PlayerViewModel>()
            val adapter = FeedAdapter(viewModel, listener, takeFullScreen)
            observe(viewModel.pagingFlow) {
                adapter.saveState()
                adapter.submitData(it)
            }
            observe(playerViewModel.playerState.current) { adapter.onCurrentChanged(it) }
            return adapter
        }

        fun getTouchHelper(listener: FeedClickListener) = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START) {
                override fun getMovementFlags(
                    recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                ): Int {
                    if (viewHolder !is MediaViewHolder) return 0
                    if (viewHolder.feed?.item !is Track) return 0
                    return makeMovementFlags(0, ItemTouchHelper.START)
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val feed = (viewHolder as MediaViewHolder).feed ?: return
                    val track = feed.item as? Track ?: return
                    listener.onTrackSwiped(viewHolder.itemView, feed.extensionId, track)
                    viewHolder.bindingAdapter?.notifyItemChanged(viewHolder.bindingAdapterPosition)
                }

                override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder) = 0.25f
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder,
                ) = false
            }
        )
    }
}