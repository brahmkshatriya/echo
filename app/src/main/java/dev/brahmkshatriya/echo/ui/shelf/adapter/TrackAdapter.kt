package dev.brahmkshatriya.echo.ui.shelf.adapter

import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemPlaylistItemBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.PlayerState.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadInto

class TrackAdapter(
    private val listener: Listener
) : PagingDataAdapter<Track, TrackAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Track, newItem: Track) = oldItem == newItem
    }

    interface Listener {
        fun onTrackClicked(
            extensionId: String?, list: List<Track>, index: Int, context: EchoMediaItem?, view: View
        )

        fun onTrackLongClicked(
            extensionId: String?, list: List<Track>, index: Int, context: EchoMediaItem?, view: View
        )
    }

    var id: String? = null
    var context: EchoMediaItem? = null
    suspend fun submit(
        id: String?, context: EchoMediaItem?, data: PagingData<Track>
    ) {
        this.id = id
        this.context = context
        submitData(data)
    }

    var current: PlayerState.Current? = null
    fun onCurrentChanged(current: PlayerState.Current?) {
        this.current = current
        onEachViewHolder { onCurrentChanged(current) }
    }

    inner class ViewHolder(
        val binding: ItemPlaylistItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                listener.onTrackClicked(id, snapshot().items, position, context, it)
            }
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                listener.onTrackLongClicked(id, snapshot().items, position, context, it)
                true
            }
        }

        fun onCurrentChanged(current: PlayerState.Current?) {
            val position = bindingAdapterPosition
            val item = getItem(position)
            val playing = current.isPlaying(item?.id)
            binding.playlistItemNowPlaying.isVisible = playing
            (binding.playlistItemNowPlaying.drawable as Animatable).start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemPlaylistItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        val binding = holder.binding
        binding.playlistItemTitle.text = item.title
        val subtitle = item.toMediaItem().subtitleWithE
        binding.playlistItemAuthor.text = subtitle
        binding.playlistItemAuthor.isVisible = !subtitle.isNullOrBlank()
        item.cover.loadInto(binding.playlistItemImageView)
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.onCurrentChanged(current)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.onCurrentChanged(current)
    }

    var recyclerView: RecyclerView? = null
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
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
}