package dev.brahmkshatriya.echo.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemShelfMediaGridBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaTitleBinding
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.applyIsPlaying
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.bind
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.icon
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.utils.image.loadInto
import dev.brahmkshatriya.echo.utils.ui.dpToPx
import kotlin.math.roundToInt

class GridViewHolder(
    val listener: ShelfAdapter.Listener,
    val clientId: String,
    val binding: ItemShelfMediaGridBinding
) : ShelfListItemViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    override fun bind(item: Any) {
        val media = when (item) {
            is EchoMediaItem -> {
                binding.iconContainer.isVisible = true
                binding.icon.setImageResource(item.icon())
                binding.icon.isVisible = true
                binding.count.isVisible = false
                binding.root.setOnClickListener {
                    listener.onClick(clientId, item, it)
                }
                binding.root.setOnLongClickListener {
                    listener.onLongClick(clientId, item, it)
                }
                item
            }

            is Track -> {
                val pos = bindingAdapterPosition
                val shelf = shelf as? Shelf.Lists.Tracks ?: return
                val isNumbered = shelf.isNumbered
                val tracks = shelf.list
                val media = item.toMediaItem()
                binding.iconContainer.isVisible = isNumbered
                binding.icon.isVisible = false
                binding.count.isVisible = isNumbered
                binding.count.text = (pos + 1).toString()
                binding.root.setOnClickListener {
                    if (isNumbered)
                        listener.onClick(clientId, null, tracks, pos, it)
                    else listener.onClick(clientId, media, it)
                }
                binding.root.setOnLongClickListener {
                    if (isNumbered) listener.onLongClick(clientId, null, tracks, pos, it)
                    else listener.onLongClick(clientId, media, it)
                }
                media
            }

            else -> return
        }
        this.item = media
        val titleBinding = NewItemMediaTitleBinding.bind(binding.root)
        titleBinding.bind(media)
        media.cover.loadInto(binding.imageView, media.placeHolder())
    }

    var item: EchoMediaItem? = null
    override fun onCurrentChanged(current: Current?) {
        applyIsPlaying(current, item?.id, binding.isPlaying)
    }

    override val transitionView = binding.root

    companion object {
        fun create(
            parent: ViewGroup,
            listener: ShelfAdapter.Listener,
            clientId: String
        ): GridViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return GridViewHolder(
                listener,
                clientId,
                ItemShelfMediaGridBinding.inflate(inflater, parent, false)
            )
        }

        fun <T> Shelf.Lists<*>.ifGrid(block: (Shelf.Lists<*>) -> T): T? {
            return if (type == Shelf.Lists.Type.Grid) block(this)
            else null
        }

        fun View.gridItemSpanCount(horizontalPadding: Int = 16 * 2): Int {
            val itemWidth = 176.dpToPx(context)
            val screenWidth = resources.displayMetrics.widthPixels
            val newWidth = screenWidth - horizontalPadding.dpToPx(context)
            val count = (newWidth.toFloat() / itemWidth).roundToInt()
            return count
        }
    }
}