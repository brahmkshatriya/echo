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
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.TrackItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemShelfMediaBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.PlayerState.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.ui.shelf.adapter.lists.MediaItemShelfListsViewHolder.Companion.applyCover
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationYAnimation
import dev.brahmkshatriya.echo.utils.ui.UiUtils.toTimeString

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
        ) {}

        fun onTrackLongClicked(
            extensionId: String?, list: List<Track>, index: Int, context: EchoMediaItem?, view: View
        ) {}
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
        val binding: ItemShelfMediaBinding
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
            binding.coverContainer.isPlaying.isVisible = playing
            (binding.coverContainer.isPlaying.drawable as Animatable).start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemShelfMediaBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.binding.bindTrack(item, true, position)
        holder.itemView.applyTranslationYAnimation(scrollAmount)
        holder.onCurrentChanged(current)
    }

    companion object {
        fun ItemShelfMediaBinding.bindTrack(track: Track, isNumbered: Boolean, position: Int) {
            title.text = if (!isNumbered) track.title
            else root.context.getString(R.string.n_dot_x, position + 1, track.title)
            val item = track.toMediaItem()
            val subtitleText = item.subtitleWithDuration()
            subtitle.text = subtitleText
            subtitle.isVisible = !subtitleText.isNullOrBlank()
            play.isVisible = false
            coverContainer.run { applyCover(item, cover, listBg1, listBg2, icon) }
        }

        fun EchoMediaItem.subtitleWithDuration() = when (this) {
            is TrackItem -> buildString {
                track.duration?.toTimeString()?.let { append(it) }
                subtitleWithE?.let { if (isNotBlank()) append(" • $it") else append(it) }
            }

            else -> subtitleWithE
        }

    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.onCurrentChanged(current)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.onCurrentChanged(current)
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
}