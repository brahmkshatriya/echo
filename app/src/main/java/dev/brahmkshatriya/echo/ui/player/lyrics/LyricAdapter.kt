package dev.brahmkshatriya.echo.ui.player.lyrics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.databinding.ItemLyricBinding

class LyricAdapter(
    val listener: Listener
) : ListAdapter<Pair<Boolean, Lyrics.Item>, LyricAdapter.ViewHolder>(DiffCallback) {
    fun interface Listener {
        fun onLyricSelected(lyric: Lyrics.Item)
    }
    object DiffCallback : DiffUtil.ItemCallback<Pair<Boolean, Lyrics.Item>>() {
        override fun areItemsTheSame(
            oldItem: Pair<Boolean, Lyrics.Item>, newItem: Pair<Boolean, Lyrics.Item>
        ) = oldItem.second.text == newItem.second.text
        override fun areContentsTheSame(
            oldItem: Pair<Boolean, Lyrics.Item>, newItem: Pair<Boolean, Lyrics.Item>
        ) = oldItem == newItem
    }
    class ViewHolder(val binding: ItemLyricBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemLyricBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (current, lyric) = getItem(position) ?: return
        holder.binding.root.apply {
            setOnClickListener { listener.onLyricSelected(lyric) }
            text = lyric.text
            alpha = if (current) 1f else 0.33f
        }
    }
}