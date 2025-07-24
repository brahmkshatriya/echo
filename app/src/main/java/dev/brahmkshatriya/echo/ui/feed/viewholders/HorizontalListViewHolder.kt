package dev.brahmkshatriya.echo.ui.feed.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemShelfListsBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.feed.FeedClickListener
import dev.brahmkshatriya.echo.ui.feed.FeedType
import dev.brahmkshatriya.echo.ui.feed.viewholders.shelf.ShelfType
import dev.brahmkshatriya.echo.ui.feed.viewholders.shelf.ShelfViewHolder
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationAndScaleAnimation

class HorizontalListViewHolder(
    parent: ViewGroup,
    listener: FeedClickListener,
    pool: RecyclerView.RecycledViewPool,
    private val binding: ItemShelfListsBinding = ItemShelfListsBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
    )
) : FeedViewHolder<FeedType.HorizontalList>(binding.root) {
    val adapter = Adapter(listener)
    val layoutManager = LinearLayoutManager(parent.context, RecyclerView.HORIZONTAL, false)

    init {
        binding.root.setRecycledViewPool(pool)
        binding.root.layoutManager = layoutManager
    }

    override fun bind(feed: FeedType.HorizontalList) {
        adapter.tracks = feed.shelf.list.filterIsInstance<Track>()
        adapter.submitList(feed.shelf.toShelfType(feed.extensionId, feed.context, feed.tabId)) {
            binding.root.adapter = adapter
        }
    }

    override fun onCurrentChanged(current: PlayerState.Current?) {
        adapter.onCurrentChanged(current)
    }

    fun Shelf.Lists<*>.toShelfType(
        extensionId: String, context: EchoMediaItem?, tabId: String?
    ) = when (this) {
        is Shelf.Lists.Items -> list.map { ShelfType.Media(extensionId, context, tabId, it) }
        is Shelf.Lists.Categories -> list.map {
            ShelfType.Category(extensionId, context, tabId, it)
        }

        is Shelf.Lists.Tracks -> list.chunked(3).mapIndexed { index, it ->
            ShelfType.ThreeTracks(
                extensionId, context, tabId, index,
                Triple(
                    it[0],
                    it.getOrNull(1),
                    it.getOrNull(2)
                )
            )
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ShelfType>() {
        override fun areItemsTheSame(oldItem: ShelfType, newItem: ShelfType): Boolean {
            if (oldItem.extensionId != newItem.extensionId) return false
            if (oldItem.type != newItem.type) return false
            if (oldItem.id != newItem.id) return false
            return true
        }

        override fun areContentsTheSame(oldItem: ShelfType, newItem: ShelfType): Boolean {
            return oldItem == newItem
        }
    }

    class Adapter(
        private val listener: FeedClickListener
    ) : ListAdapter<ShelfType, ShelfViewHolder<*>>(DiffCallback) {
        var tracks: List<Track> = emptyList()
        override fun getItemViewType(position: Int) = currentList[position].type.ordinal
        override fun onCreateViewHolder(
            parent: ViewGroup, viewType: Int
        ) = when (ShelfType.Enum.entries[viewType]) {
            ShelfType.Enum.Category -> ShelfViewHolder.Category(parent, listener)
            ShelfType.Enum.Media -> ShelfViewHolder.Media(parent, listener)
            ShelfType.Enum.ThreeTracks -> ShelfViewHolder.ThreeTracks(
                parent, listener, { tracks }
            )
        }

        override fun onBindViewHolder(
            holder: ShelfViewHolder<*>, position: Int
        ) {
            holder.itemView.applyTranslationAndScaleAnimation(scrollAmountX)
            holder.scrollX = scrollAmountX
            when (holder) {
                is ShelfViewHolder.Category -> holder.bind(
                    position,
                    currentList.map { it as ShelfType.Category })

                is ShelfViewHolder.Media -> holder.bind(
                    position,
                    currentList.map { it as ShelfType.Media })

                is ShelfViewHolder.ThreeTracks -> holder.bind(
                    position,
                    currentList.map { it as ShelfType.ThreeTracks })
            }
            holder.onCurrentChanged(current)
        }

        var current: PlayerState.Current? = null

        override fun onViewDetachedFromWindow(holder: ShelfViewHolder<*>) {
            holder.onCurrentChanged(current)
        }

        override fun onViewAttachedToWindow(holder: ShelfViewHolder<*>) {
            holder.onCurrentChanged(current)
        }

        private var scrollAmountX: Int = 0
        private val scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                scrollAmountX = dx
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

        private fun onEachViewHolder(action: ShelfViewHolder<*>.() -> Unit) {
            recyclerView?.let { rv ->
                for (i in 0 until rv.childCount) {
                    val holder = rv.getChildViewHolder(rv.getChildAt(i)) as? ShelfViewHolder<*>
                    holder?.action()
                }
            }
        }

        fun onCurrentChanged(current: PlayerState.Current?) {
            this.current = current
            onEachViewHolder { onCurrentChanged(current) }
        }
    }
}