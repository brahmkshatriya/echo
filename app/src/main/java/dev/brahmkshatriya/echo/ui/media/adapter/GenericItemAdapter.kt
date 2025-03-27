package dev.brahmkshatriya.echo.ui.media.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.shelf.adapter.MediaItemViewHolder
import kotlinx.coroutines.flow.MutableStateFlow

class GenericItemAdapter(
    private val current: MutableStateFlow<PlayerState.Current?>,
    private val listener: MediaItemViewHolder.Listener,
) : ListAdapter<EchoMediaItem, MediaItemViewHolder.Small>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<EchoMediaItem>() {
        override fun areItemsTheSame(oldItem: EchoMediaItem, newItem: EchoMediaItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: EchoMediaItem, newItem: EchoMediaItem) =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemViewHolder.Small {
        val inflater = LayoutInflater.from(parent.context)
        return MediaItemViewHolder.Small(null, listener, inflater, parent)
    }

    var extensionId: String? = null
    fun submitList(extensionId: String?, list: List<EchoMediaItem>) {
        this.extensionId = extensionId
        onEachViewHolder { this.extensionId = extensionId }
        submitList(list)
    }

    override fun onBindViewHolder(holder: MediaItemViewHolder.Small, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(item.toShelf())
        holder.onCurrentChanged(current.value)
    }

    override fun onViewAttachedToWindow(holder: MediaItemViewHolder.Small) {
        holder.onCurrentChanged(current.value)
        holder.extensionId = extensionId
    }

    override fun onViewDetachedFromWindow(holder: MediaItemViewHolder.Small) {
        holder.onCurrentChanged(current.value)
        holder.extensionId = extensionId
    }

    fun onCurrentChanged() {
        onEachViewHolder { onCurrentChanged(current.value) }
    }

    private var recyclerView: RecyclerView? = null
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    private fun onEachViewHolder(action: MediaItemViewHolder.Small .() -> Unit) {
        recyclerView?.let { rv ->
            for (i in 0 until rv.childCount) {
                val holder = rv.getChildViewHolder(rv.getChildAt(i)) as? MediaItemViewHolder.Small
                holder?.action()
            }
        }
    }
}