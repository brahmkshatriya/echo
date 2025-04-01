package dev.brahmkshatriya.echo.ui.download

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.Progress
import dev.brahmkshatriya.echo.databinding.ItemDownloadBinding
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.db.models.ContextEntity
import dev.brahmkshatriya.echo.download.db.models.DownloadEntity
import dev.brahmkshatriya.echo.download.db.models.TaskType

class DownloadsAdapter :
    ListAdapter<DownloadsAdapter.Item, DownloadsAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            when (item) {
                is Download -> {
                    binding.title.text = item.downloadEntity.track.title
                    binding.subtitle.text = item.context?.mediaItem?.title
                }

                is Task -> {
                    binding.title.text = item.taskType.name
                    binding.subtitle.text = item.progress.toString()
                }
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return when (oldItem) {
                is Download -> if (newItem !is Download) false
                else oldItem.downloadEntity.id == newItem.downloadEntity.id

                is Task -> if (newItem !is Task) false
                else oldItem.id == newItem.id
            }
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }

    }

    sealed interface Item
    data class Download(val context: ContextEntity?, val downloadEntity: DownloadEntity) : Item
    data class Task(val taskType: TaskType, val progress: Progress, val id: Long) : Item

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemDownloadBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        fun List<Downloader.Info>.toItems() = flatMap { info ->
            val download = info.download
            listOf(Download(info.context, download)) + info.workers.map {
                Task(it.first, it.second, download.id)
            }
        }
    }
}