package dev.brahmkshatriya.echo.ui.playlist

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.ItemPlaylistItemBinding
import dev.brahmkshatriya.echo.playback.Queue.StreamableTrack
import dev.brahmkshatriya.echo.playback.toTimeString
import dev.brahmkshatriya.echo.ui.player.LifeCycleListAdapter
import dev.brahmkshatriya.echo.ui.player.PlayerTrackAdapter
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.observe
import kotlinx.coroutines.flow.Flow

class PlaylistAdapter(
    private val current: Flow<StreamableTrack?>?,
    private val callback: Callback
) : LifeCycleListAdapter<StreamableTrack, ItemPlaylistItemBinding>(PlayerTrackAdapter.DiffCallback) {

    open class Callback {
        open fun onItemClicked(position: Int) {}
        open fun onItemClosedClicked(position: Int) {}
        open fun onDragHandleTouched(viewHolder: RecyclerView.ViewHolder) {}
    }

    override fun inflateCallback(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = ItemPlaylistItemBinding.inflate(inflater, container, false)

    @SuppressLint("ClickableViewAccessibility")
    override fun Holder<StreamableTrack, ItemPlaylistItemBinding>.onBind(position: Int) {
        val item = getItem(position)
        val track = item.unloaded
        binding.playlistItemTitle.text = track.title
        track.cover.loadInto(binding.playlistItemImageView, R.drawable.art_music)
        var subtitle = ""
        track.duration?.toTimeString()?.let {
            subtitle += it
        }
        track.artists.joinToString(", ") { it.name }.let {
            if (it.isNotBlank()) subtitle += if (subtitle.isNotBlank()) " • $it" else it
        }
        binding.playlistItemAuthor.isVisible = subtitle.isNotEmpty()
        binding.playlistItemAuthor.text = subtitle

        binding.playlistItemClose.setOnClickListener {
            callback.onItemClosedClicked(bindingAdapterPosition)
        }

        binding.playlistItemDragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked != MotionEvent.ACTION_DOWN) return@setOnTouchListener false
            callback.onDragHandleTouched(this)
            true
        }

        binding.root.setOnClickListener {
            callback.onItemClicked(bindingAdapterPosition)
        }

        current?.let { currentFlow ->
            observe(currentFlow) {
                binding.playlistCurrentItem.isVisible = it?.unloaded?.id == item.unloaded.id
            }
        } ?: { binding.playlistCurrentItem.isVisible = false }
    }


}
