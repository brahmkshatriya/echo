package dev.brahmkshatriya.echo.ui.download

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.progressindicator.CircularProgressIndicator
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.databinding.ItemDownloadTaskBinding
import dev.brahmkshatriya.echo.databinding.ItemDownloadTrackBinding
import dev.brahmkshatriya.echo.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.db.models.Status.Cancelled
import dev.brahmkshatriya.echo.db.models.Status.Completed
import dev.brahmkshatriya.echo.db.models.Status.Failed
import dev.brahmkshatriya.echo.db.models.Status.Initialized
import dev.brahmkshatriya.echo.db.models.Status.Paused
import dev.brahmkshatriya.echo.db.models.Status.Progressing
import dev.brahmkshatriya.echo.db.models.TaskType
import dev.brahmkshatriya.echo.utils.image.loadAsCircle
import dev.brahmkshatriya.echo.utils.image.loadInto

class DownloadItemAdapter(
    private val listener: Listener
) : ListAdapter<DownloadItem, DownloadItemAdapter.ViewHolder>(DownloadItem.Diff) {

    sealed class ViewHolder(val itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract val playPause: MaterialCheckBox
        abstract val progress: CircularProgressIndicator
        abstract val cancel: Button
        abstract val listener: Listener

        private var item: DownloadItem? = null
        private val playPauseListener = object : MaterialCheckBox.OnCheckedStateChangedListener {
            override fun onCheckedStateChangedListener(checkBox: MaterialCheckBox, state: Int) {
                val item = item ?: return
                if (checkBox.isChecked) listener.onResumeClick(item.taskIds)
                else listener.onPauseClick(item.taskIds)
            }
        }

        private val cancelListener = View.OnClickListener {
            val item = item ?: return@OnClickListener
            if (item.taskIds.isNotEmpty()) listener.onCancelClick(item.taskIds)
            else listener.onCancelClick(item.trackId)
        }

        @CallSuper
        open fun bind(item: DownloadItem) {
            this.item = item
            playPause.run {
                removeOnCheckedStateChangedListener(playPauseListener)
                isVisible = when(item) {
                    is DownloadItem.Task -> item.supportsPausing
                    is DownloadItem.Track -> if (item.taskIds.size > 1) item.supportsPausing else false
                }
                isChecked = item.isPlaying
                addOnCheckedStateChangedListener(playPauseListener)
            }
            itemView.alpha = if (item.taskIds.isEmpty()) 0.66f else 1f
            progress.run {
                isVisible =  when(item) {
                    is DownloadItem.Task -> item.taskEntity.status != Failed
                    is DownloadItem.Track -> item.taskIds.size > 1 && item.tasks.any { it.status != Failed }
                }
                isIndeterminate = item.total == null
                progress = item.progress.toInt()
                max = item.total?.toInt() ?: item.progress.toInt()
            }
            cancel.isVisible = when(item) {
                is DownloadItem.Task -> true
                is DownloadItem.Track -> item.taskIds.size != 1
            }
            cancel.setOnClickListener(cancelListener)
        }

        class Track(
            val binding: ItemDownloadTrackBinding,
            override val listener: Listener,
            override val playPause: MaterialCheckBox = binding.trackPlayPause,
            override val progress: CircularProgressIndicator = binding.trackProgressBar,
            override val cancel: Button = binding.trackCancel
        ) : ViewHolder(binding.root) {
            override fun bind(item: DownloadItem) {
                super.bind(item)

                val trackItem = item as? DownloadItem.Track ?: return
                binding.trackExt.run {
                    trackItem.extension?.iconUrl?.toImageHolder()
                        .loadAsCircle(this, R.drawable.ic_extension) { setImageDrawable(it) }
                }
                val track = trackItem.trackEntity.track
                track.cover.loadInto(binding.trackCover, R.drawable.art_music)
                binding.trackTitle.text = track.title
                binding.trackContext.text = trackItem.context?.title
            }
        }

        class Task(
            val binding: ItemDownloadTaskBinding,
            override val listener: Listener,
            override val playPause: MaterialCheckBox = binding.taskPlayPause,
            override val progress: CircularProgressIndicator = binding.taskProgressBar,
            override val cancel: Button = binding.taskCancel,
        ) : ViewHolder(binding.root) {

            override fun bind(item: DownloadItem) {
                super.bind(item)
                val taskItem = item as? DownloadItem.Task ?: return
                val task = taskItem.taskEntity
                binding.taskTitle.text = getTaskType(task)
                binding.taskSubtitle.text = getStatusString(task)
            }

            private fun getTaskType(task: MediaTaskEntity) = inContext {
                when (task.type) {
                    TaskType.METADATA -> getString(R.string.metadata)
                    TaskType.MERGE -> getString(R.string.merging)
                    TaskType.TAGGING -> getString(R.string.tagging)
                    TaskType.DOWNLOAD ->
                        getString(R.string.downloading_x, task.title ?: task.id.toString())
                }
            }

            private fun getStatusString(task: MediaTaskEntity) = inContext {
                when (task.status) {
                    Initialized -> getString(R.string.initialized)
                    Completed -> getString(R.string.completed)
                    Cancelled -> getString(R.string.cancelled)
                    Failed -> getString(R.string.failed)
                    Paused -> getString(R.string.paused_x, getProgress(task.progress, task.size))
                    Progressing ->
                        getString(R.string.progress_x, getProgress(task.progress, task.size))
                }
            }

            private fun <T> inContext(block: Context.() -> T): T = itemView.context.block()
        }

        fun getProgress(progress: Long, total: Long?): Int {
            if (total == null) return 99
            return (progress * 100.0 / total).toInt()
        }
    }

    interface Listener {
        fun onCancelClick(taskIds: List<Long>)
        fun onPauseClick(taskIds: List<Long>)
        fun onResumeClick(taskIds: List<Long>)
        fun onCancelClick(trackId: Long)
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is DownloadItem.Track -> 0
            is DownloadItem.Task -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> ViewHolder.Track(
                ItemDownloadTrackBinding.inflate(inflater, parent, false), listener
            )

            else -> ViewHolder.Task(
                ItemDownloadTaskBinding.inflate(inflater, parent, false), listener
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}