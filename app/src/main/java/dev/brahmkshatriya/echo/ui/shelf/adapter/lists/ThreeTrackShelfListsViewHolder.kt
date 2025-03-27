package dev.brahmkshatriya.echo.ui.shelf.adapter.lists

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfListsFourTracksBinding
import dev.brahmkshatriya.echo.ui.media.adapter.TrackAdapter.Companion.bindTrack
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationYAnimation
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import kotlin.math.sign

class ThreeTrackShelfListsViewHolder(
    listener: ShelfListsAdapter.Listener,
    inflater: LayoutInflater,
    parent: ViewGroup,
    val binding: ItemShelfListsFourTracksBinding =
        ItemShelfListsFourTracksBinding.inflate(inflater, parent, false)
) : ShelfListsAdapter.ViewHolder(binding.root) {

    companion object {
        const val MULTIPLIER = 3
        const val DELAY = 50L
    }

    private val bindings = listOf(binding.track1, binding.track2, binding.track3)

    init {
        bindings.forEachIndexed { index, mediaBinding ->
            mediaBinding.coverContainer.cover.clipToOutline = true
            mediaBinding.root.setOnClickListener {
                val position = bindingAdapterPosition * MULTIPLIER + index
                val shelf = shelf ?: return@setOnClickListener
                if (!shelf.isNumbered) listener.onTrackClicked(
                    extensionId, listOfNotNull(shelf.list.getOrNull(position)), 0, null, it
                )
                else listener.onTrackClicked(
                    extensionId, shelf.list, position, null, it
                )
            }
            mediaBinding.root.setOnLongClickListener {
                val position = bindingAdapterPosition * MULTIPLIER + index
                val shelf = shelf ?: return@setOnLongClickListener false
                if (!shelf.isNumbered) listener.onTrackLongClicked(
                    extensionId, listOfNotNull(shelf.list.getOrNull(position)), 0, null, it
                )
                else listener.onTrackLongClicked(
                    extensionId, shelf.list, position, null, it
                )
                true
            }
            mediaBinding.play.isVisible = false
            mediaBinding.more.setOnClickListener {
                val position = bindingAdapterPosition * MULTIPLIER + index
                val shelf = shelf ?: return@setOnClickListener
                listener.onMediaItemLongClicked(
                    extensionId, shelf.list.getOrNull(position)?.toMediaItem(), it
                )
            }
        }
        binding.root.updateLayoutParams {
            val maxWidth = 320.dpToPx(binding.root.context)
            width = maxWidth
        }
    }

    var shelf: Shelf.Lists.Tracks? = null
    override fun bind(shelf: Shelf.Lists<*>?, position: Int, xScroll: Int, yScroll: Int) {
        val tracks = shelf as? Shelf.Lists.Tracks ?: return
        this.shelf = tracks
        val animateDelayReserved = yScroll.sign < 0
        bindings.forEachIndexed { index, view ->
            val trackPos = position * MULTIPLIER + index
            val track = tracks.list.getOrNull(trackPos)
            view.root.isVisible = track != null
            if (track == null) return@forEachIndexed
            view.bindTrack(track, tracks.isNumbered, trackPos)
            val animationDelay = if (animateDelayReserved) DELAY * (MULTIPLIER - index)
            else DELAY * index
            view.root.applyTranslationYAnimation(yScroll, animationDelay)
        }
    }
}