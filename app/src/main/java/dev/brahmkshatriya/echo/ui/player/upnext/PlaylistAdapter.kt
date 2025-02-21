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
import dev.brahmkshatriya.echo.databinding.ItemPlaylistItemBinding
import dev.brahmkshatriya.echo.databinding.SkeletonItemQueueBinding

class PlaylistAdapter(
    private val listener: Listener,
    private val inactive: Boolean = false
) : ListAdapter<Pair<Boolean?, MediaItem>, PlaylistAdapter.ViewHolder>(DiffCallback) {

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
        val binding: ItemPlaylistItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.playlistItemClose.setOnClickListener {
                listener.onItemClosedClicked(bindingAdapterPosition)
            }

            binding.root.setOnClickListener {
                listener.onItemClicked(bindingAdapterPosition)
            }

            binding.playlistItemDragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked != MotionEvent.ACTION_DOWN) return@setOnTouchListener false
                listener.onDragHandleTouched(this)
                true
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemPlaylistItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(position)
    }

    private fun ViewHolder.onBind(position: Int) {
        val (current, item) = getItem(position)
        val isCurrent = current != null
        val isPlaying = current == true
//        val track = item.track

        binding.playlistItem.alpha = if (inactive) 0.5f else 1f

        binding.playlistItemTitle.text = item.mediaMetadata.title//track.title
//        track.cover.loadInto(binding.playlistItemImageView, R.drawable.art_music)
        var subtitle = ""
//        track.duration?.toTimeString()?.let {
//            subtitle += it
//        }
//        track.artists.joinToString(", ") { it.name }.let {
//            if (it.isNotBlank()) subtitle += if (subtitle.isNotBlank()) " • $it" else it
//        }
        binding.playlistItemAuthor.isVisible = subtitle.isNotEmpty()
        binding.playlistItemAuthor.text = subtitle

        binding.playlistItemClose.isVisible = !inactive
        binding.playlistItemDragImg.isVisible = !inactive

        binding.playlistCurrentItem.isVisible = isCurrent
//        binding.playlistProgressBar.isVisible = isCurrent && !item.isLoaded
        binding.playlistItemNowPlaying.isVisible = isPlaying
        (binding.playlistItemNowPlaying.drawable as Animatable).start()
    }

    class Loader : RecyclerView.Adapter<Loader.ViewHolder>() {
        inner class ViewHolder(binding: SkeletonItemQueueBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = SkeletonItemQueueBinding.inflate(inflater, parent, false)
            return ViewHolder(binding)
        }

        private var loading = false
        override fun getItemCount() = if (loading) 1 else 0
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
        fun setLoading(loading: Boolean) {
            if (this.loading == loading) return
            this.loading = loading
            if (loading) notifyItemInserted(0) else notifyItemRemoved(0)
        }
    }
}
