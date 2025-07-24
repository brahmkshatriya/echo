package dev.brahmkshatriya.echo.ui.playlist.edit

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemPlaylistTrackBinding
import dev.brahmkshatriya.echo.ui.player.more.upnext.QueueAdapter.Companion.bind
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimListAdapter

class PlaylistTrackAdapter(
    private val listener: Listener,
) : ScrollAnimListAdapter<Track, PlaylistTrackAdapter.ViewHolder>(DiffCallback) {
    object DiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Track, newItem: Track) = oldItem == newItem
    }

    interface Listener {
        fun onTrackClicked(viewHolder: ViewHolder)
        fun onTrackClosedClicked(viewHolder: ViewHolder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent, listener)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val track = getItem(position)
        holder.bind(track)
    }

    class ViewHolder(
        parent: ViewGroup,
        listener: Listener,
        val binding: ItemPlaylistTrackBinding = ItemPlaylistTrackBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    ) : RecyclerView.ViewHolder(binding.root) {
        var track: Track? = null

        init {
            binding.playlistItemClose.setOnClickListener {
                listener.onTrackClosedClicked(this)
            }
            binding.playlistItemDrag.setOnTouchListener { v, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    v.performClick()
                    listener.onTrackClicked(this)
                }
                true
            }
            val color = MaterialColors.getColor(binding.root, R.attr.echoBackground)
            binding.root.backgroundTintList = ColorStateList.valueOf(color)
            binding.playlistItemNowPlaying.isVisible = false
            binding.playlistItem.updatePaddingRelative(start = 24.dpToPx(binding.root.context))
        }

        fun bind(track: Track) {
            this.track = track
            binding.bind(track)
        }
    }

    companion object {
        fun getTouchHelperAndListener(
            viewModel: EditPlaylistViewModel
        ): Pair<Listener, ItemTouchHelper> {
            val callback = object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.START
            ) {
                override fun getMovementFlags(
                    recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder
                ): Int {
                    if (viewHolder !is ViewHolder) return 0
                    return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.START)
                }
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    if (viewHolder !is ViewHolder) return false
                    if (target !is ViewHolder) return false

                    val fromPos = viewHolder.bindingAdapterPosition
                    val toPos = target.bindingAdapterPosition
                    viewModel.edit(EditPlaylistViewModel.Action.Move(fromPos, toPos))
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val pos = viewHolder.bindingAdapterPosition
                    viewModel.edit(EditPlaylistViewModel.Action.Remove(listOf(pos)))
                }
            }
            val itemTouchHelper = ItemTouchHelper(callback)
            val listener = object : Listener {
                override fun onTrackClicked(viewHolder: ViewHolder) {
                    itemTouchHelper.startDrag(viewHolder)
                }

                override fun onTrackClosedClicked(viewHolder: ViewHolder) {
                    viewModel.edit(EditPlaylistViewModel.Action.Remove(listOf(viewHolder.bindingAdapterPosition)))
                }
            }
            return listener to itemTouchHelper
        }
    }
}