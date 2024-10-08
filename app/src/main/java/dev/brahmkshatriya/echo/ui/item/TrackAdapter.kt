package dev.brahmkshatriya.echo.ui.item

import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.ui.adapter.TrackViewHolder
import kotlinx.coroutines.flow.StateFlow

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
        val current: StateFlow<Current?>
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

    private fun destroyLifeCycle(holder: TrackViewHolder) {
        if (holder.lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED))
            holder.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    @CallSuper
    override fun onViewRecycled(holder: TrackViewHolder) {
        destroyLifeCycle(holder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val holder = TrackViewHolder.create(parent, listener, clientId, context)
        holder.lifecycleRegistry = LifecycleRegistry(holder)
        return holder
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        destroyLifeCycle(holder)
        holder.lifecycleRegistry = LifecycleRegistry(holder)
        holder.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        val binding = holder.binding
        val track = getItem(position) ?: return
        binding.root.transitionName = (transition + track.id).hashCode().toString()
        val lists = snapshot().items
        val shelf = Shelf.Lists.Tracks("", lists, isNumbered = isNumbered)
        holder.shelf = shelf
        holder.bind(track)
    }
}