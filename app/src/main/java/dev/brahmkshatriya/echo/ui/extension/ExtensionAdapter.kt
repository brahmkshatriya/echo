package dev.brahmkshatriya.echo.ui.extension

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.databinding.ItemExtensionBinding
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.ui.adapter.ShelfEmptyAdapter
import dev.brahmkshatriya.echo.utils.loadWith

class
ExtensionAdapter(
    val listener: Listener
) : PagingDataAdapter<Metadata, ExtensionAdapter.ViewHolder>(DiffCallback) {

    fun interface Listener {
        fun onClick(metadata: Metadata, view: View)
    }

    object DiffCallback : DiffUtil.ItemCallback<Metadata>() {
        override fun areItemsTheSame(
            oldItem: Metadata, newItem: Metadata
        ) = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: Metadata, newItem: Metadata
        ) = oldItem == newItem
    }

    private val empty = ShelfEmptyAdapter()
    fun withEmptyAdapter() = ConcatAdapter(empty, this)

    class ViewHolder(val binding: ItemExtensionBinding, val listener: Listener) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(metadata: Metadata) {
            binding.root.transitionName = metadata.id
            binding.root.setOnClickListener { listener.onClick(metadata, binding.root) }
            binding.extensionName.apply {
                text = if (metadata.enabled) metadata.name
                else context.getString(R.string.extension_disabled, metadata.name)
            }
            binding.extensionVersion.text = "${metadata.version} • ${metadata.importType.name}"
            binding.itemExtension.apply {
                metadata.iconUrl?.toImageHolder().loadWith(this, R.drawable.ic_extension) {
                    setImageDrawable(it)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemExtensionBinding.inflate(inflater, parent, false), listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val download = getItem(position) ?: return
        holder.bind(download)
    }

    suspend fun submit(list: List<Metadata>) {
        empty.loadState = if (list.isEmpty()) LoadState.Loading else LoadState.NotLoading(true)
        submitData(PagingData.from(list))
    }
}