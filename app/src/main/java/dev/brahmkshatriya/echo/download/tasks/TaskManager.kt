package dev.brahmkshatriya.echo.download.tasks

import dev.brahmkshatriya.echo.download.Downloader
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskManager(private val downloader: Downloader) {
    val scope = downloader.scope
    private val downloadFlow = downloader.downloadFlow
    private val context = downloader.app.context

    val taskFlow = MutableStateFlow(listOf<TaskItem>())

    val progressFlow = taskFlow.map { works ->
        works.flatMap { it.queue }.flatMap { it.workers }
    }.flatMapCombineLatest { workers ->
        workers.filter { it.running }.map { worker ->
            worker.throttledProgressFlow.map { worker to it }
        }
    }.stateIn(scope, SharingStarted.Eagerly, arrayOf())

    data class TaskItem(
        val trackId: Long,
        val queue: List<QueueItem>,
        val job: Job,
    )

    data class QueueItem(val workers: List<BaseTask>)

    companion object {
        fun BaseTask.toQueueItem() = QueueItem(listOf(this))
        fun List<BaseTask>.toQueueItem() = QueueItem(this)
    }

    fun enqueue(trackId: Long, items: List<QueueItem>) {
        val list = taskFlow.value.toMutableList()
        list.add(TaskItem(trackId, items, scope.launch {
            runCatching {
                items.onEach { item ->
                    val results = item.workers.map { async { it.doWork() } }.awaitAll()
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

    private inline fun <T, reified R> Flow<T>.flatMapCombineLatest(
        crossinline transform: (T) -> List<Flow<R>>
    ): Flow<Array<R>> = channelFlow {
        collectLatest { t ->
            val flows = transform(t)
            if (flows.isEmpty()) send(emptyArray())
            else combine(flows) { it }.collectLatest { send(it) }
        }
    }

    fun removeAll() {
        taskFlow.value.forEach { it.job.cancel() }
        taskFlow.value = listOf()
    }
}
