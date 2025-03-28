package dev.brahmkshatriya.echo.ui.playlist.edit

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemPlaylistTrackBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.PlayerState.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.ui.media.adapter.TrackAdapter
import dev.brahmkshatriya.echo.ui.player.upnext.QueueAdapter.Companion.bind
import dev.brahmkshatriya.echo.ui.player.upnext.QueueAdapter.Companion.isPlaying
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationYAnimation
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import kotlinx.coroutines.flow.MutableStateFlow

class PlaylistAdapter(
    private val current: MutableStateFlow<PlayerState.Current?>,
    private val listener: Listener,
) : ListAdapter<Track, PlaylistAdapter.ViewHolder>(TrackAdapter.DiffCallback) {
    interface Listener {
        fun onItemClosedClicked(viewHolder: ViewHolder)
        fun onDragHandleTouched(viewHolder: ViewHolder)
    }

    inner class ViewHolder(
        val binding: ItemPlaylistTrackBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.playlistItemClose.setOnClickListener {
                listener.onItemClosedClicked(this)
            }
            binding.playlistItemDrag.setOnTouchListener { v, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    v.performClick()
                    listener.onDragHandleTouched(this)
                }
                true
            }
            val color = MaterialColors.getColor(binding.root, R.attr.echoBackground)
            binding.root.backgroundTintList = ColorStateList.valueOf(color)
            binding.playlistItem.updatePaddingRelative(start = 24.dpToPx(binding.root.context))
        }

        var track: Track? = null
        fun bind(position: Int) {
            val track = getItem(position) ?: return
            this.track = track
            binding.bind(track)
        }

        fun onCurrentChanged(current: PlayerState.Current?) {
            binding.isPlaying(current.isPlaying(track?.id))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemPlaylistTrackBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
        holder.onCurrentChanged(current.value)
        holder.binding.root.applyTranslationYAnimation(scrollAmount)
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.onCurrentChanged(current.value)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.onCurrentChanged(current.value)
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

    fun onCurrentChanged() {
        onEachViewHolder { onCurrentChanged(current.value) }
    }

    companion object {
        fun getTouchHelper(
            viewModel: EditPlaylistViewModel
        ): ItemTouchHelper {
            val callback = object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.START
            ) {
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
            return ItemTouchHelper(callback)
        }
    }
}