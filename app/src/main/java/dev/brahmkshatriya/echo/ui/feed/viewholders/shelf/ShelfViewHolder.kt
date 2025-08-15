package dev.brahmkshatriya.echo.ui.feed.viewholders.shelf

import android.graphics.drawable.Animatable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemShelfCategoryBinding
import dev.brahmkshatriya.echo.databinding.ItemShelfListsMediaBinding
import dev.brahmkshatriya.echo.databinding.ItemShelfListsThreeTracksBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.PlayerState.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.ui.feed.FeedClickListener
import dev.brahmkshatriya.echo.ui.feed.viewholders.CategoryViewHolder.Companion.bind
import dev.brahmkshatriya.echo.ui.feed.viewholders.MediaViewHolder.Companion.applyCover
import dev.brahmkshatriya.echo.ui.feed.viewholders.MediaViewHolder.Companion.bind

sealed class ShelfViewHolder<T : ShelfType>(view: View) : RecyclerView.ViewHolder(view) {
    var scrollX = 0
    abstract fun bind(index: Int, list: List<T>)
    open fun onCurrentChanged(current: PlayerState.Current?) {}

    class Category(
        parent: ViewGroup,
        listener: FeedClickListener,
        private val binding: ItemShelfCategoryBinding = ItemShelfCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    ) : ShelfViewHolder<ShelfType.Category>(binding.root) {

        private var item: ShelfType.Category? = null

        init {
            binding.root.setOnClickListener {
                listener.openFeed(
                    it,
                    item?.extensionId,
                    item?.id,
                    item?.category?.title,
                    item?.category?.subtitle,
                    item?.category?.feed
                )
            }
            binding.root.updateLayoutParams { width = WRAP_CONTENT }
            binding.icon.clipToOutline = true
        }

        override fun bind(index: Int, list: List<ShelfType.Category>) {
            val item = list[index]
            this.item = item
            binding.bind(item.category)
        }
    }

    class Media(
        parent: ViewGroup,
        listener: FeedClickListener,
        private val binding: ItemShelfListsMediaBinding = ItemShelfListsMediaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    ) : ShelfViewHolder<ShelfType.Media>(binding.root) {
        var shelf: ShelfType.Media? = null

        init {
            binding.coverContainer.cover.clipToOutline = true
            binding.root.setOnClickListener {
                when (val item = shelf?.media) {
                    is Track -> {
                        listener.onTracksClicked(
                            it,
                            shelf?.extensionId,
                            shelf?.context,
                            listOf(item),
                            0
                        )
                    }

                    else -> listener.onMediaClicked(it, shelf?.extensionId, item, shelf?.context)
                }
            }
            binding.root.setOnLongClickListener {
                listener.onMediaLongClicked(
                    it, shelf?.extensionId, shelf?.media,
                    shelf?.context, shelf?.tabId, bindingAdapterPosition
                )
                true
            }
        }

        override fun bind(index: Int, list: List<ShelfType.Media>) {
            val shelf = list[index]
            this.shelf = shelf
            binding.bind(shelf.media)
        }

        override fun onCurrentChanged(current: PlayerState.Current?) {
            val isPlaying = current.isPlaying(shelf?.media?.id)
            binding.coverContainer.isPlaying.isVisible = isPlaying
            (binding.coverContainer.isPlaying.icon as Animatable).start()
        }
    }

    class ThreeTracks(
        parent: ViewGroup,
        listener: FeedClickListener,
        getAllTracks: () -> List<Track>,
        binding: ItemShelfListsThreeTracksBinding = ItemShelfListsThreeTracksBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    ) : ShelfViewHolder<ShelfType.ThreeTracks>(binding.root) {
        private val bindings = listOf(binding.track1, binding.track2, binding.track3)

        private var shelf: ShelfType.ThreeTracks? = null

        init {
            bindings.forEachIndexed { index, binding ->
                binding.coverContainer.cover.clipToOutline = true
                val actualIndex = shelf?.number?.let { it * 3 + index } ?: index
                binding.root.setOnClickListener { view ->
                    val tracks = getAllTracks()
                    val pos = shelf?.number?.let { it * 3 + index } ?: index
                    listener.onTracksClicked(view, shelf?.extensionId, shelf?.context, tracks, pos)
                }
                binding.root.setOnLongClickListener {
                    listener.onMediaLongClicked(
                        it, shelf?.extensionId, shelf?.tracks?.toList()?.getOrNull(index),
                        shelf?.context, shelf?.tabId, actualIndex
                    )
                    true
                }
                binding.more.setOnClickListener {
                    listener.onMediaLongClicked(
                        it, shelf?.extensionId, shelf?.tracks?.toList()?.getOrNull(index),
                        shelf?.context, shelf?.tabId, actualIndex
                    )
                }
            }
        }

        override fun bind(index: Int, list: List<ShelfType.ThreeTracks>) {
            val shelf = list[index]
            this.shelf = shelf
            val number = shelf.number
            val tracks = shelf.tracks.toList()
            bindings.forEachIndexed { index, view ->
                val track = tracks.getOrNull(index)
                view.root.isVisible = track != null
                if (track == null) return@forEachIndexed
                view.bind(track, number?.let { it * 3 + index })
            }
        }

        override fun onCurrentChanged(current: PlayerState.Current?) {
            val tracks = shelf?.tracks?.toList() ?: return
            bindings.forEachIndexed { index, binding ->
                val track = tracks.getOrNull(index) ?: return@forEachIndexed
                val isPlaying = current.isPlaying(track.id)
                binding.coverContainer.isPlaying.isVisible = isPlaying
                (binding.coverContainer.isPlaying.icon as Animatable).start()
            }
        }
    }

    companion object {
        fun ItemShelfListsMediaBinding.bind(item: EchoMediaItem) {
            val gravity = if (item is Artist) Gravity.CENTER else Gravity.NO_GRAVITY
            title.text = item.title
            title.gravity = gravity
            subtitle.text = item.subtitleWithE
            subtitle.gravity = gravity
            subtitle.isVisible = !item.subtitleWithE.isNullOrBlank()
            coverContainer.run { applyCover(item, cover, listBg1, listBg2, icon) }
        }
    }
}