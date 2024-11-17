package dev.brahmkshatriya.echo.ui.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemShelfMediaGridBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaTitleBinding
import dev.brahmkshatriya.echo.playback.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.bind
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.icon
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.toolTipOnClick
import dev.brahmkshatriya.echo.utils.animateVisibility
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.observe

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
                binding.isPlaying.toolTipOnClick()
                observe(listener.current) {
                    val playing = it.isPlaying(item.id)
                    binding.isPlaying.animateVisibility(playing)
                    if (playing) (binding.isPlaying.icon as Animatable).start()
                }
                item
            }

            is Track -> {
                val pos = bindingAdapterPosition
                val shelf = shelf as Shelf.Lists.Tracks
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
                binding.isPlaying.toolTipOnClick()
                observe(listener.current) {
                    val playing = it.isPlaying(media.id)
                    binding.isPlaying.animateVisibility(playing)
                    if (playing) (binding.isPlaying.icon as Animatable).start()
                }
                media
            }

            else -> return
        }
        val titleBinding = NewItemMediaTitleBinding.bind(binding.root)
        titleBinding.bind(media)
        media.cover.loadInto(binding.imageView, media.placeHolder())
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
    }
}