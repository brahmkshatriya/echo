package dev.brahmkshatriya.echo.ui.download

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
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.databinding.ItemDownloadBinding
import dev.brahmkshatriya.echo.databinding.ItemDownloadGroupBinding
import dev.brahmkshatriya.echo.ui.adapter.ShelfEmptyAdapter
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.utils.image.loadInto
import dev.brahmkshatriya.echo.utils.image.loadAsCircle

class DownloadingAdapter(
    val listener: Listener
) : PagingDataAdapter<DownloadItem, DownloadingAdapter.ViewHolder>(DiffCallback) {

    interface Listener {
        fun onDownloadItemClick(download: DownloadItem.Single)
        fun onGroupToggled(download: DownloadItem.Group, checked: Boolean)
        fun onDownloadingToggled(download: DownloadItem.Single, isDownloading: Boolean)
        fun onDownloadRemove(download: DownloadItem.Single)
    }

    object DiffCallback : DiffUtil.ItemCallback<DownloadItem>() {

        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            return if (oldItem is DownloadItem.Single && newItem is DownloadItem.Single)
                oldItem.id == newItem.id
            else false
        }

        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            return oldItem == newItem
        }
    }

    private val empty = ShelfEmptyAdapter()
    fun withEmptyAdapter() = ConcatAdapter(empty, this)

    sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        abstract fun bind(download: DownloadItem)
        class Single(val binding: ItemDownloadBinding, val listener: Listener) :
            ViewHolder(binding.root) {
            override fun bind(download: DownloadItem) {
                download as DownloadItem.Single
                binding.downloadTitle.text = download.item.title
                download.item.cover?.loadInto(binding.itemImageView, download.item.placeHolder())
                binding.itemExtension.apply {
                    download.clientIcon?.toImageHolder().loadAsCircle(this, R.drawable.ic_extension) {
                        setImageDrawable(it)
                    }
                }
                binding.itemContainer.setOnClickListener { listener.onDownloadItemClick(download) }
                binding.downloadClose.setOnClickListener { listener.onDownloadRemove(download) }
                binding.downloadPlayPause.isChecked = download.isDownloading
                binding.downloadPlayPause.addOnCheckedStateChangedListener { it, _ ->
                    listener.onDownloadingToggled(download, it.isChecked)
                }
                binding.downloadProgressBar.progress = download.progress
//                binding.downloadProgressText.apply {
//                    text = context.getString(R.string.downloaded_percentage, download.progress)
//                }
                binding.downloadGroup.isVisible = download.groupName != null
            }
        }

        class Group(val binding: ItemDownloadGroupBinding, val listener: Listener) :
            ViewHolder(binding.root) {
            override fun bind(download: DownloadItem) {
                download as DownloadItem.Group
                binding.downloadGroup.text = download.name
                binding.downloadGroupToggle.isChecked = download.areChildrenVisible
                binding.downloadGroupToggle.addOnCheckedStateChangedListener { it, _ ->
                    listener.onGroupToggled(download, it.isChecked)
                }
            }

        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DownloadItem.Single -> 0
            is DownloadItem.Group -> 1
            else -> throw IllegalArgumentException("Unknown View Type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> ViewHolder.Single(
                ItemDownloadBinding.inflate(inflater, parent, false), listener
            )

            1 -> ViewHolder.Group(
                ItemDownloadGroupBinding.inflate(inflater, parent, false), listener
            )

            else -> throw IllegalArgumentException("Unknown View Type")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val download = getItem(position) ?: return
        holder.bind(download)
    }

    suspend fun submit(list: List<DownloadItem>) {
        empty.loadState = if (list.isEmpty()) LoadState.Loading else LoadState.NotLoading(true)
        submitData(PagingData.from(list))
    }
}