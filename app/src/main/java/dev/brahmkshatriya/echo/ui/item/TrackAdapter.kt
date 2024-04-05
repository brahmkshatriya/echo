package dev.brahmkshatriya.echo.ui.item

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemTrackSmallBinding
import dev.brahmkshatriya.echo.player.toTimeString
import dev.brahmkshatriya.echo.utils.loadInto

class TrackAdapter(
    private val listener: Listener,
) : RecyclerView.Adapter<TrackAdapter.ViewHolder>() {

    interface Listener {
        fun onClick(list: List<Track>, position: Int, view: View)
        fun onLongClick(list: List<Track>, position: Int, view: View): Boolean
    }

    private var list: List<Track>? = null

    inner class ViewHolder(val binding: ItemTrackSmallBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTrackSmallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = list?.size ?: 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val track = list?.get(position) ?: return
        binding.itemNumber.text =
            binding.root.context.getString(R.string.number_dot, (position + 1))
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

        binding.root.transitionName = track.id

        binding.root.setOnClickListener {
            val list = list ?: return@setOnClickListener
            listener.onClick(list, position, binding.root)
        }
        binding.root.setOnLongClickListener {
            val list = list ?: return@setOnLongClickListener false
            listener.onLongClick(list, position, binding.root)
        }
        binding.itemMore.setOnClickListener {
            val list = list ?: return@setOnClickListener
            listener.onLongClick(list, position, binding.root)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(tracks: List<Track>) {
        list = tracks
        notifyDataSetChanged()
    }
}