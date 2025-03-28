package dev.brahmkshatriya.echo.ui.player.upnext

import android.annotation.SuppressLint
import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemPlaylistTrackBinding
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadInto
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationYAnimation
import dev.brahmkshatriya.echo.utils.ui.UiUtils.marquee
import dev.brahmkshatriya.echo.utils.ui.UiUtils.toTimeString

class QueueAdapter(
    private val listener: Listener,
    private val inactive: Boolean = false
) : ListAdapter<Pair<Boolean?, MediaItem>, QueueAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Pair<Boolean?, MediaItem>>() {
        override fun areItemsTheSame(
            oldItem: Pair<Boolean?, MediaItem>,
            newItem: Pair<Boolean?, MediaItem>
        ) = oldItem.second.mediaId == newItem.second.mediaId

        override fun areContentsTheSame(
            oldItem: Pair<Boolean?, MediaItem>,
            newItem: Pair<Boolean?, MediaItem>
        ) = oldItem == newItem

    }

    open class Listener {
        open fun onItemClicked(position: Int) {}
        open fun onItemClosedClicked(position: Int) {}
        open fun onDragHandleTouched(viewHolder: RecyclerView.ViewHolder) {}
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class ViewHolder(
        val binding: ItemPlaylistTrackBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.playlistItemClose.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                listener.onItemClosedClicked(pos)
            }

            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                listener.onItemClicked(pos)
            }

            binding.playlistItemDrag.setOnTouchListener { _, event ->
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnTouchListener false
                if (event.actionMasked != MotionEvent.ACTION_DOWN) return@setOnTouchListener false
                listener.onDragHandleTouched(this)
                true
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemPlaylistTrackBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(position)
        holder.itemView.applyTranslationYAnimation(scrollAmount)
    }

    private fun ViewHolder.onBind(position: Int) {
        val (current, item) = getItem(position)
        val isCurrent = current != null
        val isPlaying = current == true
        val track = item.track
        binding.bind(track)
        binding.isPlaying(isPlaying)
        binding.playlistItemClose.isVisible = !inactive
        binding.playlistItemDrag.isVisible = !inactive
        binding.playlistCurrentItem.isVisible = isCurrent
        binding.playlistProgressBar.isVisible = isCurrent && !item.isLoaded
        binding.playlistItem.alpha = if (inactive) 0.5f else 1f
    }

//    class Loader : RecyclerView.Adapter<Loader.ViewHolder>() {
//        inner class ViewHolder(binding: SkeletonItemQueueBinding) :
//            RecyclerView.ViewHolder(binding.root)
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//            val inflater = LayoutInflater.from(parent.context)
//            val binding = SkeletonItemQueueBinding.inflate(inflater, parent, false)
//            return ViewHolder(binding)
//        }
//
//        private var loading = false
//        override fun getItemCount() = if (loading) 1 else 0
//        override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
//        fun setLoading(loading: Boolean) {
//            if (this.loading == loading) return
//            this.loading = loading
//            if (loading) notifyItemInserted(0) else notifyItemRemoved(0)
//        }
//    }

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

    companion object {
        fun ItemPlaylistTrackBinding.bind(track: Track) {
            playlistItemTitle.run {
                text = track.title
                marquee()
            }

            track.cover.loadInto(playlistItemImageView, R.drawable.art_music)
            val subtitle = buildString {
                track.duration?.toTimeString()?.let {
                    append("$it • ")
                }
                append(track.toMediaItem().subtitleWithE)
            }
            playlistItemAuthor.run {
                isVisible = subtitle.isNotEmpty()
                text = subtitle
                marquee()
            }
        }

        fun ItemPlaylistTrackBinding.isPlaying(isPlaying: Boolean) {
            playlistItemNowPlaying.isVisible = isPlaying
            (playlistItemNowPlaying.drawable as Animatable).start()
        }
    }
}
