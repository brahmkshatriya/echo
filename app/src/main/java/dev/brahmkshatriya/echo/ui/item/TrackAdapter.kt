package dev.brahmkshatriya.echo.ui.item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemTrackSmallBinding
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.toTimeString

class TrackAdapter(
    private val transition: String,
    private val listener: Listener,
) : PagingDataAdapter<Track, TrackAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Track, newItem: Track) = oldItem == newItem
    }

    interface Listener {
        fun onClick(list: List<Track>, position: Int, view: View)
        fun onLongClick(list: List<Track>, position: Int, view: View): Boolean
    }

    suspend fun submit(pagingData: PagingData<Track>?) {
        submitData(pagingData ?: PagingData.empty())
    }

    inner class ViewHolder(val binding: ItemTrackSmallBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTrackSmallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val track = getItem(position) ?: return
        binding.itemNumber.text =
            binding.root.context.getString(R.string.number_dot, position + 1)
        binding.itemTitle.text = track.title
        track.cover.loadInto(binding.imageView, R.drawable.art_music)
        var subtitle = ""
        track.duration?.toTimeString()?.let {
            subtitle += it
        }
        track.artists.joinToString(", ") { it.name }.let {
            if (it.isNotBlank()) subtitle += if (subtitle.isNotBlank()) " • $it" else it
        }
        binding.itemSubtitle.isVisible = subtitle.isNotEmpty()
        binding.itemSubtitle.text = subtitle

        binding.root.transitionName = (transition + track.id).hashCode().toString()

        binding.root.setOnClickListener {
            val list = snapshot().items
            listener.onClick(list, position, binding.root)
        }
        binding.root.setOnLongClickListener {
            val list = snapshot().items
            listener.onLongClick(list, position, binding.root)
        }
        binding.itemMore.setOnClickListener {
            val list = snapshot().items
            listener.onLongClick(list, position, binding.root)
        }
    }
}