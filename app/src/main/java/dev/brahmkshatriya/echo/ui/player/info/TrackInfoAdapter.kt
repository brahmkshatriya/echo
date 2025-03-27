package dev.brahmkshatriya.echo.ui.player.info

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemTrackInfoBinding
import dev.brahmkshatriya.echo.ui.media.adapter.MediaHeaderAdapter.Companion.getInfoString

class TrackInfoAdapter : RecyclerView.Adapter<TrackInfoAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTrackInfoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTrackInfoBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    private var id: String? = null
    private var track: Track? = null

    @SuppressLint("NotifyDataSetChanged")
    fun submit(id: String?, track: Track?) {
        this.id = id
        this.track = track
        notifyDataSetChanged()
    }

    override fun getItemCount() = if (track == null) 0 else 1

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = track ?: return
        val context = holder.binding.root.context
        holder.binding.root.text = track.getInfoString(true, context)
    }


}