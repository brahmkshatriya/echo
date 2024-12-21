package dev.brahmkshatriya.echo.ui.item

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.ui.adapter.TrackViewHolder
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import kotlinx.coroutines.Job

class TrackAdapter(
    private val clientId: String,
    private val transition: String,
    private val listener: Listener,
    private val context: EchoMediaItem? = null,
    private val isNumbered: Boolean = false
) : PagingDataAdapter<Track, TrackViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Track, newItem: Track) = false
    }

    interface Listener {
        fun onClick(
            clientId: String, context: EchoMediaItem?, list: List<Track>, pos: Int, view: View
        )

        fun onLongClick(
            clientId: String, context: EchoMediaItem?, list: List<Track>, pos: Int, view: View
        ): Boolean
    }

    suspend fun submit(pagingData: PagingData<Track>?) {
        submitData(pagingData ?: PagingData.empty())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val holder = TrackViewHolder.create(parent, listener, clientId, context)
        return holder
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val binding = holder.binding
        val track = getItem(position) ?: return
        binding.root.transitionName = (transition + track.id).hashCode().toString()
        val lists = snapshot().items
        val shelf = Shelf.Lists.Tracks("", lists, isNumbered = isNumbered)
        holder.shelf = shelf
        holder.bind(track)
        holder.onCurrentChanged(current)
    }

    fun applyCurrent(fragment: Fragment, recyclerView: RecyclerView): Job {
        val playerViewModel by fragment.activityViewModels<PlayerViewModel>()
        return fragment.observe(playerViewModel.currentFlow) {
            onCurrentChanged(recyclerView, it)
        }
    }

    private var current: Current? = null
    private fun onCurrentChanged(recycler: RecyclerView, current: Current?) {
        this.current = current
        for (i in 0 until recycler.childCount) {
            val holder = recycler.getChildViewHolder(recycler.getChildAt(i)) as? TrackViewHolder
            holder?.onCurrentChanged(current)
        }
    }
}