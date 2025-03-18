package dev.brahmkshatriya.echo.ui.shelf.adapter.lists

import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfListsMediaGridBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.PlayerState.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.ui.shelf.adapter.ListsShelfViewHolder.Companion.gridItemSpanCount
import dev.brahmkshatriya.echo.ui.shelf.adapter.lists.MediaItemShelfListsViewHolder.Companion.applyCover
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationYAnimation
import kotlin.math.sign

class GridMediaShelfListsViewHolder(
    listener: ShelfListsAdapter.Listener,
    inflater: LayoutInflater,
    parent: ViewGroup,
    val binding: ItemShelfListsMediaGridBinding =
        ItemShelfListsMediaGridBinding.inflate(inflater, parent, false)
) : ShelfListsAdapter.ViewHolder(binding.root) {

    init {
        binding.root.setOnClickListener {
            when (val shelf = shelf) {
                is Shelf.Lists.Items -> {
                    val item = shelf.list.getOrNull(bindingAdapterPosition)
                    if (item is EchoMediaItem.Lists.RadioItem)
                        listener.onMediaItemPlayClicked(extensionId, item, it)
                    else
                        listener.onMediaItemClicked(extensionId, item, it)
                }

                is Shelf.Lists.Tracks -> if (shelf.isNumbered) listener.onTrackClicked(
                    extensionId, shelf.list, bindingAdapterPosition, null, it
                ) else listener.onMediaItemClicked(
                    extensionId, shelf.list.getOrNull(bindingAdapterPosition)?.toMediaItem(), it
                )

                else -> Unit
            }
        }

        binding.root.setOnLongClickListener {
            when (val shelf = shelf) {
                is Shelf.Lists.Items -> listener.onMediaItemLongClicked(
                    extensionId, shelf.list.getOrNull(bindingAdapterPosition), it
                )

                is Shelf.Lists.Tracks -> if (shelf.isNumbered) listener.onTrackLongClicked(
                    extensionId, shelf.list, bindingAdapterPosition, null, it
                ) else listener.onMediaItemLongClicked(
                    extensionId, shelf.list.getOrNull(bindingAdapterPosition)?.toMediaItem(), it
                )

                else -> Unit
            }
            true
        }

        binding.coverContainer.cover.clipToOutline = true
    }

    var shelf: Shelf.Lists<*>? = null
    override fun bind(shelf: Shelf.Lists<*>?, position: Int, xScroll: Int, yScroll: Int) {
        this.shelf = shelf
        val gridCount = binding.root.context.gridItemSpanCount()
        val viewPosition = position / gridCount
        val total = (shelf?.list?.size ?: 0) / gridCount
        val animationDelay = if (yScroll.sign < 0) 50L * (total - viewPosition)
        else viewPosition * 50L
        binding.root.applyTranslationYAnimation(yScroll, animationDelay)
        val (numbered, item) = when (shelf) {
            is Shelf.Lists.Items -> false to shelf.list.getOrNull(position)
            is Shelf.Lists.Tracks -> shelf.isNumbered to shelf.list.getOrNull(position)
                ?.toMediaItem()

            else -> false to null
        }
        item ?: return
        binding.title.text = if (!numbered) item.title
        else binding.root.context.getString(R.string.n_dot_x, position + 1, item.title)
        binding.subtitle.text = item.subtitleWithE
        binding.subtitle.isVisible = !item.subtitleWithE.isNullOrBlank()
        binding.coverContainer.run { applyCover(item, cover, listBg1, listBg2, icon) }
    }

    override fun onCurrentChanged(current: PlayerState.Current?) {
        val shelf = shelf ?: return
        val position = bindingAdapterPosition
        val itemId = when (shelf) {
            is Shelf.Lists.Items -> shelf.list.getOrNull(position)?.id
            is Shelf.Lists.Tracks -> shelf.list.getOrNull(position)?.id
            else -> null
        }
        binding.coverContainer.isPlaying.isVisible = current.isPlaying(itemId)
        (binding.coverContainer.isPlaying.drawable as Animatable).start()
    }
}