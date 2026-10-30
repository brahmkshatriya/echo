package dev.brahmkshatriya.echo.app.playback

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import dev.brahmkshatriya.kaudiooutput.AudioOutputs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** App/service-owned state. Screens attach to this session and never own native players. */
@Stable
class PlayerSession(
    private val seedQueues: () -> List<QueueSeed>,
    dispatcher: CoroutineDispatcher,
    createSystemMediaSession: (PlayerSession) -> SystemMediaSession,
    createAudioOutputs: (CoroutineScope) -> AudioOutputs,
) : AutoCloseable {
    val scope =
        CoroutineScope(
            SupervisorJob() +
                dispatcher +
                CoroutineExceptionHandler { _, error -> reportError("Playback failed", error) }
        )
    val audioOutputs = createAudioOutputs(scope)
    private var nextEntryId = 0
    private var revision = 0L
    private var errorId = 0L
    private var closed = false
    var isPlaying by mutableStateOf(false)
        private set

    var radioEnabled by mutableStateOf(false)
        private set

    var currentEntryId by mutableIntStateOf(-1)
        private set

    var selection by mutableStateOf(PlayerSelection(-1, SelectionReason.Manual, 0L))
        private set

    var queues by mutableStateOf(emptyList<PlayerQueue>())
        private set

    var queueGeneration by mutableIntStateOf(0)
        private set

    private val orderState = derivedStateOf { queues.flatMap { it.entries } }
    private val idsState = derivedStateOf { orderState.value.map { it.id } }
    private val entriesByIdState = derivedStateOf { orderState.value.associateBy { it.id } }
    private val pageByIdState = derivedStateOf {
        idsState.value.withIndex().associate { it.value to it.index }
    }
    private val queueByEntryState = derivedStateOf {
        buildMap { queues.forEach { queue -> queue.entries.forEach { put(it.id, queue) } } }
    }
    val playbackOrder: List<Int>
        get() = idsState.value

    val entries: List<QueueEntry>
        get() = orderState.value

    val hasQueue: Boolean
        get() = entries.isNotEmpty()

    fun entryIdAt(page: Int): Int = playbackOrder.getOrElse(page) { -1 }

    fun indexOfEntry(id: Int): Int = pageByIdState.value[id] ?: -1

    fun entry(id: Int): QueueEntry? = entriesByIdState.value[id]

    fun mediaFor(id: Int): PlayableMedia? = entry(id)?.media

    fun queueFor(id: Int): PlayerQueue? = queueByEntryState.value[id]

    val currentMedia: PlayableMedia?
        get() = mediaFor(currentEntryId)

    private val timelines = mutableMapOf<Int, PlayerTimelineState>()
    private val resumePositions = LinkedHashMap<String, Float>()
    private var queueResumePositions: Map<String, Float> = emptyMap()
    private var favourites by mutableStateOf(emptySet<String>())
    private var selectedQualities by mutableStateOf(emptyMap<String, String>())
    val pendingErrors: List<PlaybackError>
        field = mutableStateListOf<PlaybackError>()

    val engine = PlaybackEngine(this, scope)
    val systemMedia = createSystemMediaSession(this)
    val nativePlayer
        get() = engine.player

    val isLoading: Boolean
        get() = engine.loading.value

    val volume: androidx.compose.runtime.FloatState
        get() = engine.volume

    val lastAudibleVolume: androidx.compose.runtime.FloatState
        get() = engine.lastAudibleVolume

    val bassLevel: androidx.compose.runtime.FloatState
        get() = engine.bassLevel

    val activeTimeline: Pair<Int, PlayerTimelineState>?
        get() = currentMedia?.let { currentEntryId to timelineFor(currentEntryId) }

    init {
        ensureQueue()
        scope.launch {
            audioOutputs.errors.collect { reportError("Could not change audio output", it) }
        }
        scope.launch {
            snapshotFlow {
                    val timeline = activeTimeline?.second
                    SystemMediaState(
                        currentMedia,
                        isPlaying,
                        engine.loading.value,
                        timeline?.reportedPositionMs?.toLong() ?: 0L,
                        timeline?.durationMs?.toLong() ?: 0L,
                        nextEntryId() != null,
                        currentMedia != null,
                        timeline?.playbackSpeed ?: 1.0,
                        volume.floatValue,
                        shuffleEnabled,
                        repeatMode,
                        currentEntryId.takeIf { currentMedia != null },
                        entries,
                        favourite = isFavourite(currentEntryId),
                    )
                }
                .collect(systemMedia::update)
        }
    }

    fun timelineFor(id: Int): PlayerTimelineState {
        val media = requireNotNull(mediaFor(id)) { "Queue entry $id no longer exists" }
        return timelines.getOrPut(id) {
            PlayerTimelineState(media.durationMs?.toFloat() ?: 0f).apply {
                // A duplicate entry must not inherit a sibling's in-progress playback.
                resetPosition(queueResumePositions[media.id] ?: 0f)
            }
        }
    }

    fun saveResumePosition(mediaId: String, positionMs: Float) {
        if (!positionMs.isFinite()) return
        resumePositions.remove(mediaId)
        resumePositions[mediaId] = positionMs.coerceAtLeast(0f)
        while (resumePositions.size > MAX_RESUME_POSITIONS) resumePositions.remove(
            resumePositions.keys.first()
        )
    }

    private fun clearTimelines() {
        val media = currentMedia
        val timeline = timelines[currentEntryId]
        // The engine saved acknowledged positions before releasing its decks.
        // Preserve only an outstanding seek here, which has no backend sample yet.
        if (media != null && timeline?.isAwaitingSeek == true)
            saveResumePosition(media.id, timeline.positionMs)
        timelines.clear()
    }

    fun isFavourite(id: Int): Boolean = mediaFor(id)?.id in favourites

    fun setFavourite(id: Int, favourite: Boolean) {
        val mediaId = mediaFor(id)?.id ?: return
        favourites = if (favourite) favourites + mediaId else favourites - mediaId
    }

    fun ensureQueue() {
        if (hasQueue || closed) return
        replaceQueue(seedQueues())
    }

    fun replaceQueue(seeds: List<QueueSeed>, startPlaying: Boolean = false) {
        engine.clear()
        clearTimelines()
        queueResumePositions = resumePositions.toMap()
        isPlaying = false
        queues =
            seeds
                .map { seed ->
                    PlayerQueue(
                        seed.id,
                        seed.title,
                        seed.items.map { QueueEntry(nextEntryId++, it) },
                    )
                }
                .filter { it.entries.isNotEmpty() }
        currentEntryId = playbackOrder.firstOrNull() ?: -1
        queueGeneration++
        selection = PlayerSelection(currentEntryId, SelectionReason.Manual, ++revision)
        if (startPlaying) play()
    }

    fun clearQueue() {
        if (!hasQueue) return
        pause()
        engine.clear()
        clearTimelines()
        queueResumePositions = emptyMap()
        queues = emptyList()
        currentEntryId = -1
        queueGeneration++
        selection = PlayerSelection(-1, SelectionReason.Manual, ++revision)
    }

    fun selectEntry(id: Int, automatic: Boolean = false) {
        if (entry(id) == null || closed) return
        if (currentEntryId == id && !engine.failed) return
        currentEntryId = id
        selection =
            PlayerSelection(
                id,
                if (automatic) SelectionReason.Automatic else SelectionReason.Manual,
                ++revision,
            )
        if (!automatic) engine.select(id)
    }

    fun play() {
        if (closed) return
        if (!hasQueue) ensureQueue()
        if (!hasQueue) return
        try {
            systemMedia.activate()
        } catch (error: Exception) {
            reportError("Could not activate system playback controls", error)
        }
        isPlaying = true
        engine.select(currentEntryId)
        engine.setPlaying(true)
    }

    fun pause() {
        isPlaying = false
        engine.setPlaying(false)
    }

    fun updateRadioEnabled(enabled: Boolean) {
        radioEnabled = enabled
    }

    fun togglePlayPause() {
        if (isPlaying) pause() else play()
    }

    fun next() {
        nextEntryId()?.let { selectEntry(it) }
    }

    fun previous() {
        val target = previousTarget() ?: currentEntryId.takeIf { currentMedia != null } ?: return
        if (target == currentEntryId) seekTo(0f) else selectEntry(target)
    }

    fun previousTarget(id: Int = currentEntryId, allowRestart: Boolean = true): Int? =
        if (
            allowRestart &&
                id == currentEntryId &&
                (activeTimeline?.second?.positionMs ?: 0f) > 3_000f &&
                !engine.isCrossfading
        )
            id
        else previousEntryId(id)

    fun seekTo(positionMs: Float, entryId: Int = currentEntryId) =
        engine.seekTo(entryId, positionMs)

    fun previewSeek(entryId: Int, positionMs: Float) {
        if (entry(entryId) != null) timelineFor(entryId).previewSeek(positionMs)
    }

    fun commitSeek(entryId: Int) {
        if (entry(entryId) == null) return
        val timeline = timelineFor(entryId)
        engine.seekTo(entryId, timeline.positionMs)
        timeline.finishScrub()
    }

    fun cancelScrub(entryId: Int) {
        if (entry(entryId) != null) timelineFor(entryId).cancelScrub()
    }

    fun setVolume(volume: Float) = engine.setVolume(volume)

    fun qualityFor(entryId: Int): MediaQuality? {
        val media = mediaFor(entryId) ?: return null
        return media.availableQualities.firstOrNull { it.id == selectedQualities[media.id] }
            ?: media.availableQualities.first()
    }

    fun selectQuality(entryId: Int, qualityId: String) {
        if (closed) return
        val media = mediaFor(entryId) ?: return
        if (
            media.availableQualities.none { it.id == qualityId } ||
                qualityFor(entryId)?.id == qualityId
        )
            return
        // Capture the old source's position before changing which source the engine should load.
        engine.savePositions()
        selectedQualities = selectedQualities + (media.id to qualityId)
        engine.qualityChanged(media.id)
    }

    fun selectAudioOutput(id: String?) {
        if (closed) return
        try {
            audioOutputs.select(id)
        } catch (error: Exception) {
            reportError("Could not change audio output", error)
        }
    }

    val shuffleEnabled: Boolean
        get() = queueFor(currentEntryId)?.shuffleEnabled == true

    var repeatMode: RepeatMode
        get() = queueFor(currentEntryId)?.repeatMode ?: RepeatMode.Off
        set(value) {
            updateQueue(currentEntryId) { it.copy(repeatMode = value) }
            engine.queueChanged()
        }

    private fun updateQueue(id: Int, transform: (PlayerQueue) -> PlayerQueue) {
        val queueId = queueFor(id)?.id ?: return
        queues = queues.map { if (it.id == queueId) transform(it) else it }
    }

    fun setShuffle(enabled: Boolean, currentEntryId: Int = this.currentEntryId) {
        updateQueue(currentEntryId) { queue ->
            if (queue.shuffleEnabled == enabled) queue
            else {
                val byId = queue.entries.associateBy { it.id }
                val order =
                    if (enabled) {
                        val remaining = queue.entryIds.filterNot { it == currentEntryId }
                        val shuffled = remaining.shuffled()
                        listOf(currentEntryId) +
                            if (remaining.size > 1 && shuffled == remaining) shuffled.reversed()
                            else shuffled
                    } else queue.unshuffledOrder
                queue.copy(entries = order.mapNotNull(byId::get), shuffleEnabled = enabled)
            }
        }
        engine.queueChanged()
    }

    fun moveQueueEntry(fromEntryId: Int, toIndex: Int) {
        val target = entryIdAt(toIndex)
        val from = queueFor(fromEntryId) ?: return
        if (queueFor(target)?.id != from.id) return
        updateQueue(fromEntryId) { queue ->
            val order = queue.entries.toMutableList()
            val index = order.indexOfFirst { it.id == fromEntryId }
            val to = order.indexOfFirst { it.id == target }
            if (index < 0 || to < 0 || index == to) queue
            else {
                order.add(to, order.removeAt(index))
                queue.copy(
                    entries = order,
                    unshuffledOrder =
                        if (queue.shuffleEnabled) queue.unshuffledOrder else order.map { it.id },
                )
            }
        }
        engine.queueChanged()
    }

    fun selectRadioNext(id: Int) {
        updateQueue(id) { queue ->
            val index = queue.entries.indexOfFirst { it.id == id }
            if (index < 0 || queue.entries.size <= 2) return@updateQueue queue
            val next =
                if (index == queue.entries.lastIndex) {
                    if (queue.repeatMode == RepeatMode.Off) return@updateQueue queue else 0
                } else index + 1
            val candidate = (next + 1) % queue.entries.size
            if (candidate == index) queue
            else {
                val order = queue.entries.toMutableList()
                val old = order[next]
                order[next] = order[candidate]
                order[candidate] = old
                queue.copy(
                    entries = order,
                    unshuffledOrder =
                        if (queue.shuffleEnabled) queue.unshuffledOrder else order.map { it.id },
                )
            }
        }
    }

    fun nextEntryId(id: Int = currentEntryId): Int? {
        val queue = queueFor(id) ?: return null
        val index = queue.entryIds.indexOf(id)
        if (index < queue.entries.lastIndex) return queue.entryIds[index + 1]
        if (queue.repeatMode != RepeatMode.Off) return queue.entryIds.firstOrNull()
        return queues.getOrNull(queues.indexOf(queue) + 1)?.entryIds?.firstOrNull()
    }

    fun previousEntryId(id: Int = currentEntryId): Int? {
        val queue = queueFor(id) ?: return null
        val index = queue.entryIds.indexOf(id)
        if (index > 0) return queue.entryIds[index - 1]
        if (queue.repeatMode != RepeatMode.Off) return queue.entryIds.lastOrNull()
        return queues.getOrNull(queues.indexOf(queue) - 1)?.entryIds?.lastOrNull()
    }

    fun reportError(title: String, error: Throwable, entryId: Int? = currentEntryId) {
        val media = entryId?.let(::mediaFor)
        val summary = error.message?.takeIf { it.isNotBlank() } ?: title
        pendingErrors +=
            PlaybackError(
                ++errorId,
                listOfNotNull(media?.title, summary).joinToString(": "),
                buildString {
                    appendLine(title)
                    media?.let { appendLine("Media: ${it.title}\nID: ${it.id}\nSource: ${it.uri}") }
                    appendLine()
                    append(error.stackTraceToString())
                },
            )
    }

    fun dismissError(id: Long) {
        pendingErrors.removeAll { it.id == id }
    }

    override fun close() {
        if (closed) return
        closed = true
        closeAll(
            engine::close,
            audioOutputs::close,
            systemMedia::close,
            {
                timelines.clear()
                resumePositions.clear()
                queueResumePositions = emptyMap()
            },
            { scope.cancel() },
        )
    }

    private companion object {
        const val MAX_RESUME_POSITIONS = 128
    }
}
