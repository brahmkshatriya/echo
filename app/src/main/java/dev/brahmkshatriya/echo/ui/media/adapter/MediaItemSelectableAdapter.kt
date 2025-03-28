package dev.brahmkshatriya.echo.ui.media.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.databinding.ItemMediaSelectableBinding
import dev.brahmkshatriya.echo.databinding.ItemSelectableHeaderBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.PlayerState.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.ui.shelf.adapter.lists.MediaItemShelfListsViewHolder.Companion.bind
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import kotlin.math.roundToInt

class MediaItemSelectableAdapter(
    val listener: Listener
) : ListAdapter<Pair<EchoMediaItem, Boolean>, MediaItemSelectableAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Pair<EchoMediaItem, Boolean>>() {

        override fun areItemsTheSame(
            oldItem: Pair<EchoMediaItem, Boolean>, newItem: Pair<EchoMediaItem, Boolean>
        ) = oldItem.first.id == newItem.first.id


        override fun areContentsTheSame(
            oldItem: Pair<EchoMediaItem, Boolean>, newItem: Pair<EchoMediaItem, Boolean>
        ) = oldItem == newItem

    }

    fun interface Listener {
        fun onItemSelected(selected: Boolean, item: EchoMediaItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemMediaSelectableBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.onCurrentChanged(current)
    }

    inner class ViewHolder(
        val binding: ItemMediaSelectableBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var item: EchoMediaItem? = null

        init {
            binding.media.root.setOnClickListener {
                val item = item ?: return@setOnClickListener
                listener.onItemSelected(!binding.selected.isVisible, item)
            }
            binding.media.cover.clipToOutline = true
        }

        fun bind(item: Pair<EchoMediaItem, Boolean>) {
            val mediaItem = item.first
            this.item = mediaItem
            binding.media.bind(mediaItem)
            binding.selected.isVisible = item.second
        }

        fun onCurrentChanged(current: PlayerState.Current?) {
            binding.media.isPlaying.visibility =
                if (current.isPlaying(item?.id)) View.VISIBLE else View.INVISIBLE
            (binding.media.isPlaying.icon as Animatable).start()
        }
    }

    var current: PlayerState.Current? = null
    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.onCurrentChanged(current)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.onCurrentChanged(current)
    }

    var recyclerView: RecyclerView? = null
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    private fun onEachViewHolder(action: ViewHolder.() -> Unit) {
        recyclerView?.let { rv ->
            for (i in 0 until rv.childCount) {
                val holder = rv.getChildViewHolder(rv.getChildAt(i)) as? ViewHolder
                holder?.action()
            }
        }
    }

    fun onCurrentChanged(current: PlayerState.Current?) {
        this.current = current
        onEachViewHolder { onCurrentChanged(current) }
    }

    private var header: Header? = null
    fun withHeader(
        selectAll: (Boolean) -> Unit
    ): ConcatAdapter {
        val header = Header(selectAll)
        this.header = header
        return ConcatAdapter(header, this)
    }

    override fun onCurrentListChanged(
        previousList: MutableList<Pair<EchoMediaItem, Boolean>>,
        currentList: MutableList<Pair<EchoMediaItem, Boolean>>
    ) {
        header?.submitList(
            currentList.count { it.second },
            currentList.all { it.second }
        )
    }

    class Header(
        private val onSelectAll: (Boolean) -> Unit
    ) : RecyclerView.Adapter<Header.ViewHolder>() {
        class ViewHolder(val binding: ItemSelectableHeaderBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemSelectableHeaderBinding.inflate(inflater, parent, false)
            binding.root.setOnClickListener {
                onSelectAll(!binding.selectAll.isChecked)
            }
            return ViewHolder(binding)
        }

        override fun getItemCount() = 1
        private var count = 0
        private var selected = false

        @SuppressLint("NotifyDataSetChanged")
        fun submitList(count: Int, selected: Boolean) {
            this.count = count
            this.selected = selected
            notifyDataSetChanged()
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.selected.run {
                text = context.getString(R.string.selected_n, count)
            }
            holder.binding.selectAll.isChecked = selected
        }
    }

    companion object {
        private fun View.getCount(horizontalPadding: Int = 4 * 2) = run {
            val typed = context.theme.obtainStyledAttributes(intArrayOf(R.attr.itemCoverSize))
            val itemWidth = typed.getDimensionPixelSize(typed.getIndex(0), 0)
            val newWidth = width - horizontalPadding.dpToPx(context)
            (newWidth.toFloat() / (itemWidth + 24.dpToPx(context))).roundToInt()
        }

        fun View.mediaItemSpanCount(
            block: (count: Int) -> Unit
        ) = doOnLayout {
            block(getCount(0))
        }
    }

}