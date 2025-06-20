package dev.brahmkshatriya.echo.download.tasks

import dev.brahmkshatriya.echo.common.models.Progress
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.db.models.TaskType
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class TaskManager(private val downloader: Downloader) {
    val scope = downloader.scope
    private val downloadFlow = downloader.downloadFlow
    private val context = downloader.app.context

    val taskFlow = MutableStateFlow(listOf<TaskItem>())
    private var taskSemaphores = TaskType.entries.associateWith { Semaphore(2) }
    fun setConcurrency(limit: Int) {
        taskSemaphores = TaskType.entries.associateWith { Semaphore(limit) }
    }

    val progressFlow = channelFlow<Array<Pair<BaseTask, Progress>>> {
        taskFlow.map { items ->
            items.flatMap { it.queue }.flatMap { it.tasks }
        }.collectLatest { tasks ->
            if (tasks.isEmpty()) send(emptyArray())
            else combine(tasks.map { it.running }) { _ ->
                tasks.filter { it.running.value }.map { task ->
                    task.throttledProgressFlow.map { task to it }
                }
            }.collectLatest { progressFlows ->
                if (progressFlows.isEmpty()) send(emptyArray())
                else combine(progressFlows) { it }.collectLatest { send(it) }
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, arrayOf())

    data class TaskItem(val trackId: Long, val queue: List<QueueItem>, val job: Job)
    data class QueueItem(val tasks: List<BaseTask>)

    companion object {
        fun BaseTask.toQueueItem() = QueueItem(listOf(this))
        fun List<BaseTask>.toQueueItem() = QueueItem(this)
    }

    fun enqueue(trackId: Long, items: List<QueueItem>) {
        val list = taskFlow.value.toMutableList()
        list.add(TaskItem(trackId, items, scope.launch {
            runCatching {
                items.onEach { item ->
                    val results = item.tasks.map { worker ->
                        scope.async {
                            taskSemaphores[worker.type]!!.withPermit { worker.doWork() }
                        }
                    }.awaitAll()
                    results.onEach { it.getOrThrow() }
                }
            }
            val new = taskFlow.value.toMutableList()
            new.removeAll { it.queue == items }
            taskFlow.value = new
        }))
        taskFlow.value = list
    }

    private fun enqueueLoadingWork(trackId: Long) {
        val loadingWorker = LoadingTask(context, downloader, trackId)
        enqueue(trackId, listOf(QueueItem(listOf(loadingWorker))))
    }

    suspend fun awaitCompletion() {
        downloadFlow.map { entities -> entities.filter { !it.isFinal } }.first { entities ->
            val works = taskFlow.value.map { it.trackId }
            val notStarted = entities.filter { !works.contains(it.id) }
            notStarted.forEach { enqueueLoadingWork(it.id) }
            entities.isEmpty()
        }
    }

    fun remove(trackId: Long) {
        val list = taskFlow.value.toMutableList()
        val works = list.filter { it.trackId == trackId }
        works.forEach { it.job.cancel() }
        list.removeAll(works)
        taskFlow.value = list
    }

    fun removeAll() {
        taskFlow.value.forEach { it.job.cancel() }
        taskFlow.value = listOf()
    }
}
