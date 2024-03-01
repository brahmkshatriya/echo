package dev.brahmkshatriya.echo.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemTrackSmallBinding
import dev.brahmkshatriya.echo.player.PlayerHelper.Companion.toTimeString
import dev.brahmkshatriya.echo.ui.MediaItemClickListener
import dev.brahmkshatriya.echo.utils.loadInto

class TrackAdapter(
    private val callback: MediaItemClickListener,
    private val albumVisible: Boolean = true,
) : RecyclerView.Adapter<TrackAdapter.ViewHolder>() {

    var list: List<Track>? = null

    inner class ViewHolder(val binding: ItemTrackSmallBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val track = list?.get(bindingAdapterPosition) ?: return@setOnClickListener
                callback.onClick(binding.imageView to track.toMediaItem())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTrackSmallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = list?.count() ?: 0

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
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(tracks: List<Track>) {
        list = tracks
        notifyDataSetChanged()
    }
}