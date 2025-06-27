package dev.brahmkshatriya.echo.ui.extensions.manage

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.databinding.ItemExtensionBinding
import dev.brahmkshatriya.echo.ui.shelf.adapter.other.ShelfEmptyAdapter
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle

class
ExtensionAdapter(
    val listener: Listener
) : PagingDataAdapter<Extension<*>, ExtensionAdapter.ViewHolder>(DiffCallback) {

    interface Listener {
        fun onClick(extension: Extension<*>, view: View)
        fun onDragHandleTouched(viewHolder: ViewHolder)
        fun onOpenClick(extension: Extension<*>)
    }

    object DiffCallback : DiffUtil.ItemCallback<Extension<*>>() {
        override fun areItemsTheSame(oldItem: Extension<*>, newItem: Extension<*>) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Extension<*>, newItem: Extension<*>) =
            oldItem == newItem
    }

    private val empty = ShelfEmptyAdapter()
    fun withEmptyAdapter() = ConcatAdapter(empty, this)

    class ViewHolder(val binding: ItemExtensionBinding, val listener: Listener) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(extension: Extension<*>) {
            val metadata = extension.metadata
            binding.root.transitionName = metadata.id
            binding.root.setOnClickListener { listener.onClick(extension, binding.root) }
            binding.extensionName.apply {
                text = if (metadata.isEnabled) metadata.name
                else context.getString(R.string.x_disabled, metadata.name)
            }
            binding.extensionVersion.text = "${metadata.version} • ${metadata.importType.name}"
            binding.itemExtension.apply {
                metadata.icon.loadAsCircle(this, R.drawable.ic_extension_48dp) {
                    setImageDrawable(it)
                }
            }

            binding.extensionDrag.setOnTouchListener { v, _ ->
                v.performClick()
                listener.onDragHandleTouched(this)
                true
            }

            binding.extensionOpen.isVisible = extension.type == ExtensionType.MUSIC
            binding.extensionOpen.setOnClickListener {
                listener.onOpenClick(extension)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemExtensionBinding.inflate(inflater, parent, false), listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val download = runCatching { getItem(position) }.getOrNull() ?: return
        holder.bind(download)
    }

    suspend fun submit(list: List<Extension<*>>) {
        submitData(PagingData.from(list))
        empty.loadState = if (list.isEmpty()) LoadState.Loading
        else LoadState.NotLoading(true)
    }
}