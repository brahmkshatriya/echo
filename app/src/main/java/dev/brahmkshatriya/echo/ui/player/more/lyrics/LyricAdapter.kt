package dev.brahmkshatriya.echo.ui.player.more.lyrics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.databinding.ItemLyricBinding
import dev.brahmkshatriya.echo.ui.common.UiViewModel
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.defaultPlayerColors
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationYAnimation
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimListAdapter
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimViewHolder

class LyricAdapter(
    val uiViewModel: UiViewModel, val listener: Listener,
) : ScrollAnimListAdapter<Lyrics.Item, LyricAdapter.ViewHolder>(DiffCallback) {
    fun interface Listener {
        fun onLyricSelected(adapter: LyricAdapter, lyric: Lyrics.Item)
    }

    object DiffCallback : DiffUtil.ItemCallback<Lyrics.Item>() {
        override fun areItemsTheSame(oldItem: Lyrics.Item, newItem: Lyrics.Item) =
            oldItem.text == newItem.text

        override fun areContentsTheSame(oldItem: Lyrics.Item, newItem: Lyrics.Item) =
            oldItem == newItem
    }

    inner class ViewHolder(val binding: ItemLyricBinding) : ScrollAnimViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val lyrics = getItem(bindingAdapterPosition) ?: return@setOnClickListener
                listener.onLyricSelected(this@LyricAdapter, lyrics)
            }
        }
    }

    private fun ViewHolder.updateColors() {
        binding.root.run {
            val colors = uiViewModel.playerColors.value ?: context.defaultPlayerColors()
            val alphaStrippedColor = colors.onBackground or -0x1000000
            setTextColor(alphaStrippedColor)
        }
    }

    private fun getItemOrNull(position: Int) = runCatching { getItem(position) }.getOrNull()

    private var currentPos = -1
    private fun ViewHolder.updateCurrent() {
        val currentTime = getItemOrNull(currentPos)?.startTime ?: 0
        val time = getItemOrNull(bindingAdapterPosition)?.startTime ?: 0
        binding.root.alpha = if (currentTime >= time) 1f else 0.5f
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemLyricBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lyric = getItem(position) ?: return
        holder.binding.root.text = lyric.text.trim().trim('\n').ifEmpty { "♪" }
        holder.updateColors()
        holder.updateCurrent()
        holder.itemView.applyTranslationYAnimation(scrollY)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.updateColors()
    }

    fun updateColors() {
        onEachViewHolder { updateColors() }
    }

    fun updateCurrent(currentPos: Int) {
        this.currentPos = currentPos
        onEachViewHolder { updateCurrent() }
    }
}