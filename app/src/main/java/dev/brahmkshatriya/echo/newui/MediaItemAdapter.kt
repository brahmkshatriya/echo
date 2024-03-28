package dev.brahmkshatriya.echo.newui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem

class MediaItemAdapter(val list: List<EchoMediaItem>) :
    RecyclerView.Adapter<MediaItemViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemViewHolder {
        return when (viewType) {
            0 -> MediaItemViewHolder.Track.create(parent)
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int) = 0

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: MediaItemViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item)
        holder.clickView.setOnClickListener {
            println("Clicked")
        }
        holder.clickView.setOnLongClickListener {
            println("Long Clicked")
            true
        }
    }
}
