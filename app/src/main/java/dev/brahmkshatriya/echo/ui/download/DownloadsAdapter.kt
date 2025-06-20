package dev.brahmkshatriya.echo.ui.download

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.models.Progress
import dev.brahmkshatriya.echo.databinding.ItemDownloadBinding
import dev.brahmkshatriya.echo.databinding.ItemDownloadTaskBinding
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.db.models.ContextEntity
import dev.brahmkshatriya.echo.download.db.models.DownloadEntity
import dev.brahmkshatriya.echo.download.db.models.TaskType
import dev.brahmkshatriya.echo.download.tasks.BaseTask.Companion.getTitle
import dev.brahmkshatriya.echo.ui.common.ExceptionUtils
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadInto

class DownloadsAdapter(
    val listener: Listener
) : ListAdapter<DownloadsAdapter.Item, DownloadsAdapter.ViewHolder>(DiffCallback) {

    interface Listener {
        fun onExceptionClicked(data: ExceptionUtils.Data)
        fun onCancel(trackId: Long)
        fun onRestart(trackId: Long)
    }

    sealed class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class DownloadViewHolder(
        private val binding: ItemDownloadBinding
    ) : ViewHolder(binding.root) {
        init {
            binding.imageView.clipToOutline = true
        }

        fun bind(item: Download) {
            val entity = item.downloadEntity
            binding.apply {
                title.text = entity.track.title
                entity.track.cover.loadInto(imageView, R.drawable.art_music)
                item.extension?.metadata?.icon?.loadAsCircle(
                    extensionIcon, R.drawable.ic_extension
                ) {
                    extensionIcon.setImageDrawable(it)
                }
                val sub = item.context?.mediaItem?.title
                subtitle.text = sub
                subtitle.isVisible = !sub.isNullOrEmpty()

                exception.text = entity.exception?.title
                exception.isVisible = exception.text.isNotEmpty()

                remove.setOnClickListener {
                    listener.onCancel(entity.id)
                }

                retry.isVisible = entity.exception != null
                retry.setOnClickListener {
                    listener.onRestart(entity.id)
                }
                root.setOnClickListener {
                    val data = entity.exception ?: return@setOnClickListener
                    listener.onExceptionClicked(data)
                }
            }
        }
    }

    inner class TaskViewHolder(
        private val binding: ItemDownloadTaskBinding
    ) : ViewHolder(binding.root) {

        fun bind(item: Task) {
            binding.apply {
                progressBar.isIndeterminate = item.progress.size == 0L
                progressBar.max = item.progress.size.toInt()
                progressBar.progress = item.progress.progress.toInt()
                subtitle.text = item.progress.toText()
                title.text = root.context.run {
                    getTitle(item.taskType, getString(R.string.download))
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
    data class Download(
        val context: ContextEntity?,
        val downloadEntity: DownloadEntity,
        val extension: Extension<*>?
    ) : Item

    data class Task(val taskType: TaskType, val progress: Progress, val id: Long) : Item


    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Download -> 0
            is Task -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> DownloadViewHolder(
                ItemDownloadBinding.inflate(inflater, parent, false)
            )

            1 -> TaskViewHolder(
                ItemDownloadTaskBinding.inflate(inflater, parent, false)
            )

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is DownloadViewHolder -> {
                val item = getItem(position) as Download
                holder.bind(item)
            }

            is TaskViewHolder -> {
                val item = getItem(position) as Task
                holder.bind(item)
            }
        }
    }

    companion object {
        fun List<Downloader.Info>.toItems(extensions: List<Extension<*>>) = filter {
            it.download.finalFile == null
        }.flatMap { info ->
            val download = info.download
            val extension = extensions.find { it.id == download.extensionId }
            listOf(Download(info.context, download, extension)) + info.workers.map {
                Task(it.first, it.second, download.id)
            }
        }

        private val SPEED_UNITS = arrayOf("", "KB", "MB", "GB")
        fun Progress.toText() = buildString {
            if (size > 0) append("%.2f%% • ".format(progress.toFloat() / size * 100))
            append(
                if (size > 0) "${convertBytes(progress)} / ${convertBytes(size)}"
                else convertBytes(progress)
            )
            if (speed > 0) append(" • ${convertBytes(speed)}/s")
        }

        private fun convertBytes(bytes: Long): String {
            var value = bytes.toFloat()
            var unitIndex = 0

            while (value >= 500 && unitIndex < SPEED_UNITS.size - 1) {
                value /= 1024
                unitIndex++
            }
            return "%.2f %s".format(value, SPEED_UNITS[unitIndex])
        }
    }
}