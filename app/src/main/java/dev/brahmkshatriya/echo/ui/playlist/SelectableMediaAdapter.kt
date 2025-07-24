package dev.brahmkshatriya.echo.ui.playlist

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.databinding.ItemMediaSelectableBinding
import dev.brahmkshatriya.echo.databinding.ItemSelectableHeaderBinding
import dev.brahmkshatriya.echo.ui.common.GridAdapter
import dev.brahmkshatriya.echo.ui.feed.viewholders.shelf.ShelfViewHolder.Companion.bind
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimListAdapter
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimRecyclerAdapter

class SelectableMediaAdapter(
    val listener: Listener
) : ScrollAnimListAdapter<Pair<EchoMediaItem, Boolean>, SelectableMediaAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<Pair<EchoMediaItem, Boolean>>() {
        override fun areItemsTheSame(
            oldItem: Pair<EchoMediaItem, Boolean>, newItem: Pair<EchoMediaItem, Boolean>
        ) = oldItem.first.id == newItem.first.id

        override fun areContentsTheSame(
            oldItem: Pair<EchoMediaItem, Boolean>, newItem: Pair<EchoMediaItem, Boolean>
        ) = oldItem == newItem
    }
), GridAdapter {

    fun interface Listener {
        fun onItemSelected(selected: Boolean, item: EchoMediaItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent, listener)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.bind(getItem(position))
    }

    class ViewHolder(
        parent: ViewGroup,
        val listener: Listener,
        val binding: ItemMediaSelectableBinding = ItemMediaSelectableBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    ) : RecyclerView.ViewHolder(binding.root) {

        private var item: EchoMediaItem? = null

        init {
            binding.media.root.setOnClickListener {
                val item = item ?: return@setOnClickListener
                listener.onItemSelected(!binding.selected.isVisible, item)
            }
            binding.media.coverContainer.cover.clipToOutline = true
            binding.media.coverContainer.isPlaying.isVisible = false
        }

        fun bind(item: Pair<EchoMediaItem, Boolean>) {
            val mediaItem = item.first
            this.item = mediaItem
            binding.media.bind(mediaItem)
            binding.selected.isVisible = item.second
        }
    }

    private var header: Header? = null
    fun withHeader(selectAll: (Boolean) -> Unit): GridAdapter.Concat {
        val header = Header(selectAll)
        this.header = header
        return GridAdapter.Concat(header, this)
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

    override val adapter = this
    override fun getSpanSize(position: Int, width: Int, count: Int) = 1

    class Header(
        private val onSelectAll: (Boolean) -> Unit
    ) : ScrollAnimRecyclerAdapter<Header.ViewHolder>(), GridAdapter {
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

        fun submitList(count: Int, selected: Boolean) {
            this.count = count
            this.selected = selected
            notifyItemChanged(0)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)
            holder.binding.selected.run {
                text = context.getString(R.string.selected_n, count)
            }
            holder.binding.selectAll.isChecked = selected
        }

        override val adapter = this
        override fun getSpanSize(position: Int, width: Int, count: Int) = count
    }
}