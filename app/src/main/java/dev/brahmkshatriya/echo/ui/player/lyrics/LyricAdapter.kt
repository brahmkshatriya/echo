package dev.brahmkshatriya.echo.ui.player.lyrics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.databinding.ItemLyricBinding
import dev.brahmkshatriya.echo.ui.UiViewModel

class LyricAdapter(
    val uiViewModel: UiViewModel,
    val listener: Listener
) : ListAdapter<Pair<Boolean, Lyrics.Item>, LyricAdapter.ViewHolder>(DiffCallback) {
    fun interface Listener {
        fun onLyricSelected(adapter: LyricAdapter, lyric: Lyrics.Item)
    }

    object DiffCallback : DiffUtil.ItemCallback<Pair<Boolean, Lyrics.Item>>() {
        override fun areItemsTheSame(
            oldItem: Pair<Boolean, Lyrics.Item>, newItem: Pair<Boolean, Lyrics.Item>
        ) = oldItem.second.text == newItem.second.text

        override fun areContentsTheSame(
            oldItem: Pair<Boolean, Lyrics.Item>, newItem: Pair<Boolean, Lyrics.Item>
        ) = oldItem == newItem
    }

    inner class ViewHolder(val binding: ItemLyricBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val lyrics = getItem(bindingAdapterPosition) ?: return@setOnClickListener
                listener.onLyricSelected(this@LyricAdapter, lyrics.second)
            }
        }
    }

    private fun ViewHolder.updateColors() {
        val colors = uiViewModel.playerColors.value
        binding.root.setTextColor(colors.onBackground)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemLyricBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (current, lyric) = getItem(position) ?: return
        holder.binding.root.apply {
            text = lyric.text.trim()
            alpha = if (current) 1f else 0.33f
        }
        holder.updateColors()
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.updateColors()
    }

    var recyclerView: RecyclerView? = null
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    private fun onEachViewHolder(block: ViewHolder.() -> Unit) {
        recyclerView?.let { rv ->
            for (i in 0 until rv.childCount) {
                val holder = rv.getChildViewHolder(rv.getChildAt(i)) as? ViewHolder ?: continue
                holder.block()
            }
        }
    }

    fun updateColors() {
        onEachViewHolder { updateColors() }
    }
}