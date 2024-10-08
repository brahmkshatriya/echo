package dev.brahmkshatriya.echo.ui.adapter

import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemTrackBinding
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.toolTipOnClick
import dev.brahmkshatriya.echo.ui.item.TrackAdapter
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.toTimeString

class TrackViewHolder(
    private val listener: TrackAdapter.Listener,
    private val clientId: String,
    private val context: EchoMediaItem?,
    val binding: ItemTrackBinding
) : ShelfListItemViewHolder(binding.root) {

    override fun bind(item: Any) {
        val shelf = shelf as? Shelf.Lists.Tracks ?: return
        val pos = bindingAdapterPosition
        val track = shelf.list[pos]
        val list = shelf.list
        val isNumbered = shelf.isNumbered
        binding.itemNumber.text =
            binding.root.context.getString(R.string.number_dot, pos + 1)
        binding.itemNumber.isVisible = isNumbered
        binding.itemTitle.text = track.title
        track.cover.loadInto(binding.imageView, R.drawable.art_music)
        var subtitle = ""
        track.duration?.toTimeString()?.let {
            subtitle += it
        }
        track.artists.joinToString(", ") { it.name }.let {
            if (it.isNotBlank()) subtitle += if (subtitle.isNotBlank()) " • $it" else it
        }
        binding.itemSubtitle.isVisible = subtitle.isNotEmpty()
        binding.itemSubtitle.text = subtitle

        binding.root.setOnClickListener {
            listener.onClick(clientId, context, list, pos, binding.root)
        }
        binding.root.setOnLongClickListener {
            listener.onLongClick(clientId, context, list, pos, binding.root)
        }
        binding.itemMore.setOnClickListener {
            listener.onLongClick(clientId, context, list, pos, binding.root)
        }
        binding.isPlaying.toolTipOnClick()
        observe(listener.current) {
            val playing = it?.mediaItem?.mediaId == track.id
            binding.isPlaying.isVisible = playing
            if(playing) (binding.isPlaying.icon as Animatable).start()
        }
    }

    override val transitionView = binding.root

    companion object {
        fun create(
            parent: ViewGroup, listener: TrackAdapter.Listener, clientId: String, context: EchoMediaItem?
        ): TrackViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return TrackViewHolder(
                listener, clientId, context, ItemTrackBinding.inflate(inflater, parent, false)
            )
        }
    }
}