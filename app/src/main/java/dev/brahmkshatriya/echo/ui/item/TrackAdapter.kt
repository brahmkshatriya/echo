package dev.brahmkshatriya.echo.ui.item

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.ui.adapter.TrackViewHolder

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        TrackViewHolder.create(parent, listener, clientId, context)

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val binding = holder.binding
        val track = getItem(position) ?: return
        binding.root.transitionName = (transition + track.id).hashCode().toString()
        val lists = snapshot().items
        val shelf = Shelf.Lists.Tracks("", lists, isNumbered = isNumbered)
        holder.shelf = shelf
        holder.bind(track)
    }
}