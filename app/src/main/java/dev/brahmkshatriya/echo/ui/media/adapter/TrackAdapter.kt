package dev.brahmkshatriya.echo.ui.media.adapter

import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.TrackItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemShelfMediaBinding
import dev.brahmkshatriya.echo.databinding.SkeletonTrackListBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.PlayerState.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.ui.common.PagingUtils
import dev.brahmkshatriya.echo.ui.shelf.adapter.lists.MediaItemShelfListsViewHolder.Companion.applyCover
import dev.brahmkshatriya.echo.ui.shelf.adapter.other.ShelfLoadingAdapter
import dev.brahmkshatriya.echo.ui.shelf.adapter.other.ShelfLoadingAdapter.Companion.createListener
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationYAnimation
import dev.brahmkshatriya.echo.utils.ui.UiUtils.toTimeString
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow

class TrackAdapter(
    private val listener: Listener
) : PagingDataAdapter<Track, TrackAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Track, newItem: Track) = oldItem == newItem
    }

    interface Listener {
        fun onTrackClicked(
            extensionId: String?, list: List<Track>, index: Int, context: EchoMediaItem?, view: View
        ) {
        }

        fun onTrackLongClicked(
            extensionId: String?, list: List<Track>, index: Int, context: EchoMediaItem?, view: View
        ) {
        }

        fun onTrackSwiped(
            extensionId: String?, list: List<Track>, index: Int, context: EchoMediaItem?, view: View
        ) {
        }
    }

    private var id: String? = null
    private var context: EchoMediaItem? = null
    private var paged: PagedData<Track>? = null
    suspend fun submit(
        id: String?, context: EchoMediaItem?, paged: PagedData<Track>?, data: PagingData<Track>?
    ) {
        this.id = id
        this.context = context
        this.paged = paged
        val page = data ?: PagingData.empty()
        submitData(page)
    }

    var current: PlayerState.Current? = null
    fun onCurrentChanged(current: PlayerState.Current?) {
        this.current = current
        onEachViewHolder { onCurrentChanged(current) }
    }

    inner class ViewHolder(
        val binding: ItemShelfMediaBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.coverContainer.cover.clipToOutline = true
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                listener.onTrackClicked(id, snapshot().items, position, context, it)
            }
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                listener.onTrackLongClicked(id, snapshot().items, position, context, it)
                true
            }
            binding.more.setOnClickListener {
                binding.root.performLongClick()
            }
        }

        fun onCurrentChanged(current: PlayerState.Current?) {
            val position = bindingAdapterPosition
            val item = if (position >= 0) getItem(position) else null
            val playing = current.isPlaying(item?.id)
            binding.coverContainer.isPlaying.isVisible = playing
            (binding.coverContainer.isPlaying.drawable as Animatable).start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemShelfMediaBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.binding.bindTrack(item, true, position)
        holder.itemView.applyTranslationYAnimation(scrollAmount)
        holder.onCurrentChanged(current)
    }

    companion object {
        fun ItemShelfMediaBinding.bindTrack(track: Track, isNumbered: Boolean, position: Int) {
            title.text = if (!isNumbered) track.title
            else root.context.getString(R.string.n_dot_x, position + 1, track.title)
            val item = track.toMediaItem()
            val subtitleText = item.subtitleWithDuration()
            subtitle.text = subtitleText
            subtitle.isVisible = !subtitleText.isNullOrBlank()
            play.isVisible = false
            coverContainer.run { applyCover(item, cover, listBg1, listBg2, icon) }
        }

        fun EchoMediaItem.subtitleWithDuration() = when (this) {
            is TrackItem -> buildString {
                track.duration?.toTimeString()?.let { append(it) }
                subtitleWithE?.let { if (isNotBlank()) append(" • $it") else append(it) }
            }

            else -> subtitleWithE
        }

    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.onCurrentChanged(current)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.onCurrentChanged(current)
    }

    private var scrollAmount: Int = 0
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            scrollAmount = dy
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

    fun getTouchHelper(): ItemTouchHelper {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START) {
            override fun getMovementFlags(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder
            ): Int {
                return if (viewHolder.bindingAdapter != this@TrackAdapter) 0
                else makeMovementFlags(0, ItemTouchHelper.START)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                val list = snapshot().items
                listener.onTrackSwiped(id, list, pos, context, viewHolder.itemView)
                notifyItemChanged(pos)
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

    fun withHeaders(
        fragment: Fragment,
        viewModel: ViewModel,
        stateFlow: MutableStateFlow<PagingUtils.Data<Track>>,
        jobFlow: MutableStateFlow<Job?>,
    ): ConcatAdapter {
        val listener = fragment.createListener { retry() }
        val bottom = ShelfLoadingAdapter(TrackAdapter::Loading, listener)
        val top = ShelfLoadingAdapter(TrackAdapter::Loading, listener)
        val header = SearchHeaderAdapter(fragment, viewModel, stateFlow, jobFlow)
        addOnPagesUpdatedListener {
            val visible = paged != null
            header.submit(visible)
        }
        addLoadStateListener { loadStates ->
            top.loadState = loadStates.refresh
            bottom.loadState = loadStates.append
        }
        return ConcatAdapter(header, top, this, bottom)
    }

    data class Loading(
        val inflater: LayoutInflater,
        val parent: ViewGroup,
        val binding: SkeletonTrackListBinding =
            SkeletonTrackListBinding.inflate(inflater, parent, false)
    ) : ShelfLoadingAdapter.ViewHolder(binding.root)

}