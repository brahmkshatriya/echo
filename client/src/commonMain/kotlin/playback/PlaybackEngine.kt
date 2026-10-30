package dev.brahmkshatriya.echo.app.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import dev.brahmkshatriya.kmplayer.KMAudioAnalyzerConfig
import dev.brahmkshatriya.kmplayer.KMCacheConfig
import dev.brahmkshatriya.kmplayer.KMMediaStatus
import dev.brahmkshatriya.kmplayer.KMPlayer
import dev.brahmkshatriya.kmplayer.KMPlayerConfig
import dev.brahmkshatriya.kmplayer.KMPlayerState
import dev.brahmkshatriya.kmplayer.KMResult
import dev.brahmkshatriya.kmplayer.getOrThrow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val CrossfadeDurationMs = 10_000L
private const val PreloadFadeHeadroomMs = 5_000L

/** Native state and analyzer events drive playback. Only an active fade needs a frame timer. */
class PlaybackEngine(private val session: PlayerSession, private val scope: CoroutineScope) :
    AutoCloseable {
    private inner class Deck(val player: KMPlayer) : AutoCloseable {
        private val audioOutputBinding = bindAudioOutput(session.audioOutputs, player)
        val analyzer =
            (player.createAudioAnalyzer(
                    KMAudioAnalyzerConfig(fftSize = 2048, updateRateHz = 30, smoothing = 0.45f)
                ) as? KMResult.Success)
                ?.value
        var entry: QueueEntry? = null
        var source: MediaQuality? = null
        var timeline: PlayerTimelineState? = null
        var published = false
        var gain = 0f
        var appliedVolume = -1.0
        var commandedPlaying = false
        var handledEnd = false
        val stateJob =
            scope.launch {
                player.state.collect { state ->
                    try {
                        onNativeState(this@Deck, state)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        fail("Playback failed", error, entry?.id)
                    }
                }
            }
        val analyzerJob =
            analyzer?.let { source -> scope.launch { source.frames.collect { publishBass() } } }

        fun ready(): Boolean =
            player.state.value.let {
                it.uri == source?.uri &&
                    source?.uri == session.qualityFor(entry?.id ?: -1)?.uri &&
                    it.mediaStatus == KMMediaStatus.Ready &&
                    !it.isBuffering
            }

        fun setPlaying(playing: Boolean) {
            if (commandedPlaying == playing) return
            (if (playing) player.play() else player.pause()).getOrThrow()
            commandedPlaying = playing
        }

        fun stop() {
            setPlaying(false)
            player.stop().getOrThrow()
            entry = null
            source = null
            timeline = null
            published = false
            gain = 0f
            handledEnd = false
        }

        private var closed = false

        override fun close() {
            if (closed) return
            closed = true
            closeAll(
                { stateJob.cancel() },
                { analyzerJob?.cancel() },
                { analyzer?.close() },
                audioOutputBinding::close,
                player::close,
            )
        }
    }

    private class Fade(var elapsedMs: Double = 0.0)

    private var decks = emptyList<Deck>()
    private var selected by mutableStateOf<Deck?>(null)
    val player: KMPlayer?
        get() = selected?.player

    val loading = mutableStateOf(false)
    val bassLevel = mutableFloatStateOf(0f)
    val volume = mutableFloatStateOf(1f)
    val lastAudibleVolume = mutableFloatStateOf(1f)
    var failed by mutableStateOf(false)
        private set

    private var incoming by mutableStateOf<Deck?>(null)
    val isCrossfading: Boolean
        get() = incoming != null

    private var fade: Fade? = null
    private var requestedEntry: Int? = null
    private var switchJob: Job? = null
    private var transitionJob: Job? = null
    private var preloadJob: Job? = null
    private var preloadEntry: Int? = null
    private var preloadStartJob: Job? = null
    private var autoStartJob: Job? = null
    private var fadeJob: Job? = null
    private val completedEntries = mutableSetOf<Int>()
    private val failedPreloads = mutableSetOf<String>()
    private var generation = 0L
    private var closed = false
    private var ducking = 1f
    private val seekObservation =
        scope.launch {
            snapshotFlow { session.activeTimeline?.second?.isSeeking == true }
                .collect { seeking ->
                    if (seeking) cancelAutomaticTransition()
                    else {
                        scheduleNextPreload()
                        scheduleAutomaticTransition()
                    }
                }
        }

    fun setVolume(value: Float) {
        volume.floatValue = value.coerceIn(0f, 1f)
        if (volume.floatValue > 0f) lastAudibleVolume.floatValue = volume.floatValue
        applyVolumes()
    }

    fun setDucking(value: Float) {
        ducking = value.coerceIn(0f, 1f)
        applyVolumes()
    }

    private fun applyVolumes() {
        decks.forEach { deck ->
            val value = (volume.floatValue * ducking * deck.gain * 100.0).coerceIn(0.0, 100.0)
            if (
                abs(value - deck.appliedVolume) >= 0.05 || value == 0.0 && deck.appliedVolume != 0.0
            ) {
                val result = deck.player.setVolume(value)
                deck.appliedVolume = value
                if (result is KMResult.Failure)
                    session.reportError(
                        "Could not change the audio volume",
                        IllegalStateException(result.error.message, result.error.cause),
                        deck.entry?.id,
                    )
            }
        }
    }

    private fun savePosition(deck: Deck?) {
        val state = deck?.player?.state?.value ?: return
        val entry = deck.entry ?: return
        val timeline = deck.timeline ?: return
        if (state.uri != deck.source?.uri || timeline.isAwaitingSeek) return
        if (state.mediaStatus == KMMediaStatus.Ended) {
            completedEntries += entry.id
            session.saveResumePosition(entry.media.id, 0f)
        } else if (deck.published) {
            timeline.updateBackend(state)
            session.saveResumePosition(entry.media.id, timeline.positionMs)
        }
    }

    fun savePositions() {
        savePosition(selected)
        savePosition(incoming)
    }

    fun qualityChanged(mediaId: String) {
        if (session.currentMedia?.id == mediaId) select(session.currentEntryId, forceReload = true)
        else {
            // A cached/preloading deck must not keep the old encoding of this item.
            if (incoming?.entry?.media?.id == mediaId) cancelAutomaticTransition()
            preloadJob?.cancel()
            preloadEntry = null
            scheduleNextPreload()
        }
    }

    fun select(id: Int, forceReload: Boolean = false) {
        if (closed) return
        val entry = session.entry(id) ?: return clear()
        if (
            !forceReload &&
                requestedEntry == id &&
                !failed &&
                (switchJob?.isActive == true || selected?.published == true)
        )
            return
        if (decks.isEmpty()) {
            if (!session.isPlaying) return
            val errors = mutableListOf<Throwable>()
            decks =
                List(2) {
                        when (
                            val result =
                                KMPlayer.create(
                                    KMPlayerConfig(
                                        requireHls = false,
                                        audioOnly = null,
                                        cache = KMCacheConfig(cacheName = "echo-media"),
                                    )
                                )
                        ) {
                            is KMResult.Success -> Deck(result.value)
                            is KMResult.Failure -> {
                                errors +=
                                    IllegalStateException(result.error.message, result.error.cause)
                                null
                            }
                        }
                    }
                    .filterNotNull()
            if (decks.isEmpty()) {
                fail(
                    "Could not create the media player",
                    errors.firstOrNull() ?: IllegalStateException("No playback backend"),
                    id,
                )
                return
            }
        }
        savePosition(selected)
        savePosition(incoming)
        if (completedEntries.remove(entry.id) && !forceReload)
            session.timelineFor(id).resetPosition()
        val epoch = ++generation
        val oldSwitch = switchJob
        val oldPreload = preloadJob
        oldSwitch?.cancel()
        oldPreload?.cancel()
        cancelAutomaticTransition()
        preloadJob = null
        preloadEntry = null
        failedPreloads.clear()
        requestedEntry = id
        failed = false
        loading.value = true
        switchJob =
            scope.launch {
                try {
                    oldSwitch?.join()
                    oldPreload?.join()
                    val deck =
                        decks.firstOrNull { it.entry?.id == id }
                            ?: decks.firstOrNull { it !== selected }
                            ?: decks.first()
                    decks.filter { it !== deck }.forEach { it.stop() }
                    deck.setPlaying(false)
                    deck.published = false
                    selected = deck
                    deck.gain = 0f
                    applyVolumes()
                    prepare(deck, entry)
                    restorePosition(deck)
                    currentCoroutineContext().ensureActive()
                    if (generation != epoch) return@launch
                    deck.timeline?.beginSelection()
                    deck.published = true
                    deck.handledEnd = false
                    deck.gain = 1f
                    loading.value = false
                    deck.setPlaying(session.isPlaying)
                    applyVolumes()
                    onNativeState(deck, deck.player.state.value)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    if (generation == epoch) fail("Could not play media", error, id)
                }
            }
    }

    fun setPlaying(playing: Boolean) {
        try {
            val current = selected?.takeIf { it.published }
            if (playing && current?.player?.state?.value?.mediaStatus == KMMediaStatus.Ended) {
                current.handledEnd = false
                onEnded(current)
            } else current?.setPlaying(playing)
            incoming?.takeIf { fade != null }?.setPlaying(playing)
            if (!playing) {
                preloadStartJob?.cancel()
                preloadStartJob = null
                autoStartJob?.cancel()
                autoStartJob = null
                bassLevel.floatValue = 0f
            } else {
                scheduleNextPreload()
                scheduleAutomaticTransition()
            }
        } catch (error: Exception) {
            fail("Could not change playback", error, session.currentEntryId)
        }
    }

    fun seekTo(id: Int, positionMs: Float) {
        val timeline = session.entry(id)?.let { session.timelineFor(id) } ?: return
        cancelAutomaticTransition()
        val deck = selected?.takeIf { it.entry?.id == id && it.published }
        val state = deck?.player?.state?.value
        if (
            deck == null ||
                state == null ||
                state.uri != deck.source?.uri ||
                state.mediaStatus !in listOf(KMMediaStatus.Ready, KMMediaStatus.Ended) ||
                !state.seekable
        ) {
            timeline.deferSeek(positionMs)
            return
        }
        try {
            timeline.beginSeek(positionMs)
            deck.handledEnd = false
            deck.player.seekTo(timeline.positionMs.toLong().milliseconds).getOrThrow()
            if (session.isPlaying && state.mediaStatus == KMMediaStatus.Ended)
                deck.player.play().getOrThrow()
        } catch (error: Exception) {
            timeline.cancelPendingSeek()
            session.reportError("Could not seek", error, id)
        }
    }

    private suspend fun prepare(deck: Deck, entry: QueueEntry) {
        val media = entry.media
        val source = requireNotNull(session.qualityFor(entry.id))
        val state = deck.player.state.value
        deck.timeline = session.timelineFor(entry.id)
        if (
            deck.entry?.id != entry.id ||
                state.uri != source.uri ||
                state.mediaStatus in
                    listOf(
                        KMMediaStatus.Idle,
                        KMMediaStatus.Error,
                        KMMediaStatus.Released,
                        KMMediaStatus.Ended,
                    )
        ) {
            deck.setPlaying(false)
            deck.entry = entry
            deck.source = source
            deck.published = false
            deck.handledEnd = false
            deck.player
                .load(source.uri, playWhenReady = false, mimeType = source.mimeType)
                .getOrThrow()
        }
        val ready =
            withTimeoutOrNull(45_000L.milliseconds) {
                deck.player.state.first {
                    it.uri == source.uri &&
                        (it.mediaStatus == KMMediaStatus.Error ||
                            it.mediaStatus == KMMediaStatus.Ready && !it.isBuffering)
                }
            } ?: error("Timed out opening ${media.title}")
        if (ready.mediaStatus != KMMediaStatus.Error) return
        val error = ready.error
        throw IllegalStateException(error?.message ?: "Unable to open ${media.title}", error?.cause)
    }

    private suspend fun restorePosition(deck: Deck) {
        val timeline = requireNotNull(deck.timeline)
        val entry = requireNotNull(deck.entry)
        do {
            snapshotFlow { timeline.isSeeking }.first { !it }
            val duration =
                deck.player.duration?.inWholeMilliseconds?.toFloat()?.takeIf { it > 0f }
                    ?: timeline.durationMs.takeIf { it > 0f }
                    ?: Float.MAX_VALUE
            val deferred = timeline.deferredSeekMs
            val target =
                (deferred ?: timeline.positionMs)
                    .let { if (entry.id in completedEntries) 0f else it }
                    .coerceIn(0f, duration)
                    .toLong()
            if (abs(deck.player.position.inWholeMilliseconds - target) > 100L) {
                deck.player.seekTo(target.milliseconds).getOrThrow()
                val result =
                    withTimeoutOrNull(10_000L.milliseconds) {
                        deck.player.state.first {
                            it.mediaStatus == KMMediaStatus.Error ||
                                (it.mediaStatus in
                                    listOf(KMMediaStatus.Ready, KMMediaStatus.Ended) &&
                                    !it.isBuffering &&
                                    abs(it.position.inWholeMilliseconds - target) <= 250L)
                        }
                    }
                check(result != null && result.mediaStatus != KMMediaStatus.Error) {
                    "Could not restore playback position"
                }
            }
            // A saved/queued seek can exceed the duration discovered by the
            // decoder. Acknowledge its clamped value instead of retrying forever.
            if (timeline.deferredSeekMs == deferred) timeline.cancelPendingSeek()
        } while (timeline.deferredSeekMs != null)
    }

    private fun onNativeState(deck: Deck, state: KMPlayerState) {
        if (closed || deck.entry == null) return
        if (deck === selected && deck.published && deck.entry?.id == session.currentEntryId) {
            loading.value = state.isBuffering || state.mediaStatus == KMMediaStatus.Opening
            if (state.mediaStatus == KMMediaStatus.Error) {
                if (!failed)
                    fail(
                        "Playback failed",
                        IllegalStateException(
                            state.error?.message ?: "The media player stopped unexpectedly",
                            state.error?.cause,
                        ),
                        deck.entry?.id,
                    )
                return
            }
            val timeline = deck.timeline ?: return
            if (
                timeline.deferredSeekMs != null &&
                    !timeline.isSeeking &&
                    state.seekable &&
                    state.mediaStatus == KMMediaStatus.Ready
            )
                seekTo(session.currentEntryId, timeline.deferredSeekMs!!)
            timeline.updateBackend(state)
            if (state.mediaStatus == KMMediaStatus.Ended && !timeline.isAwaitingSeek) onEnded(deck)
            else {
                if (state.mediaStatus == KMMediaStatus.Ready) deck.handledEnd = false
                scheduleNextPreload()
                scheduleAutomaticTransition()
            }
        } else if (deck === incoming && state.mediaStatus == KMMediaStatus.Error) {
            val id = deck.entry?.id
            cancelAutomaticTransition()
            session.reportError(
                "Could not transition to the next item",
                IllegalStateException(
                    state.error?.message ?: "The next item failed",
                    state.error?.cause,
                ),
                id,
            )
            selected?.let {
                if (it.player.state.value.mediaStatus == KMMediaStatus.Ended) {
                    it.handledEnd = false
                    session.pause()
                }
            }
        }
    }

    private fun onEnded(deck: Deck) {
        if (deck.handledEnd && incoming == null) return
        val entry = requireNotNull(deck.entry)
        completedEntries += entry.id
        session.saveResumePosition(entry.media.id, 0f)
        val next = session.nextEntryId()
        if (session.repeatMode == RepeatMode.One || next == session.currentEntryId) {
            if (!session.isPlaying) return
            deck.handledEnd = true
            completedEntries.remove(deck.entry?.id)
            seekTo(session.currentEntryId, 0f)
            deck.player.play().getOrThrow()
            return
        }
        if (incoming != null && fade != null) {
            finishTransition(deck, incoming!!)
            return
        }
        if (transitionJob?.isActive == true || !session.isPlaying) return
        deck.handledEnd = true
        if (next == null) session.pause() else startAutomaticTransition(deck, next)
    }

    fun queueChanged() {
        if (
            incoming != null &&
                (session.repeatMode == RepeatMode.One ||
                    incoming?.entry?.id != session.nextEntryId())
        )
            cancelAutomaticTransition()
        scheduleNextPreload()
        scheduleAutomaticTransition()
    }

    private fun scheduleNextPreload() {
        preloadStartJob?.cancel()
        preloadStartJob = null
        val current = selected ?: return
        if (!current.published || incoming != null) return
        val id =
            session.nextEntryId()?.takeUnless {
                it == session.currentEntryId || session.repeatMode == RepeatMode.One
            }
        if (preloadEntry != null && preloadEntry != id) {
            preloadJob?.cancel()
            preloadJob = null
            preloadEntry = null
            decks
                .firstOrNull { it !== current }
                ?.let { standby ->
                    val staleId = standby.entry?.id
                    runCatching { standby.stop() }
                        .onFailure { session.reportError("Could not stop preloading", it, staleId) }
                }
        }
        if (id == null || preloadEntry == id) return
        val entry = session.entry(id) ?: return
        if (
            entry.media.id in failedPreloads ||
                decks.none { it !== current } ||
                !session.isPlaying ||
                current.timeline?.isAwaitingSeek == true
        )
            return
        val state = current.player.state.value
        if (!state.isPlaying || !current.ready()) return
        val durationMs = state.duration?.inWholeMilliseconds?.takeIf { it > 0L } ?: return
        val remainingMs = (durationMs - state.position.inWholeMilliseconds).coerceAtLeast(0L)
        // Whichever arrives first: the last fifth, or enough time to prepare
        // the next item five seconds before the automatic fade begins.
        val thresholdMs = maxOf(durationMs / 5, CrossfadeDurationMs + PreloadFadeHeadroomMs)
        val waitMs =
            ((remainingMs - thresholdMs).coerceAtLeast(0L) / state.speed.coerceAtLeast(0.01))
                .toLong()
        if (waitMs == 0L) preloadNext(current, entry)
        else
            preloadStartJob =
                scope.launch {
                    delay(waitMs.milliseconds)
                    preloadStartJob = null
                    if (
                        selected === current &&
                            current.published &&
                            incoming == null &&
                            session.isPlaying &&
                            session.repeatMode != RepeatMode.One &&
                            session.nextEntryId() == id &&
                            current.timeline?.isAwaitingSeek != true &&
                            current.ready() &&
                            current.player.state.value.isPlaying
                    )
                        preloadNext(current, entry)
                }
    }

    private fun preloadNext(current: Deck, entry: QueueEntry) {
        val standby = decks.firstOrNull { it !== current } ?: return
        val previousPreload = preloadJob
        previousPreload?.cancel()
        preloadEntry = entry.id
        preloadJob =
            scope.launch {
                try {
                    previousPreload?.join()
                    standby.gain = 0f
                    applyVolumes()
                    prepare(standby, entry)
                    restorePosition(standby)
                    scheduleAutomaticTransition()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    failedPreloads += entry.media.id
                    standby.stop()
                    session.reportError("Could not prepare the next item", error, entry.id)
                }
            }
    }

    private fun scheduleAutomaticTransition() {
        autoStartJob?.cancel()
        autoStartJob = null
        val current = selected ?: return
        if (
            !current.published ||
                incoming != null ||
                !session.isPlaying ||
                session.repeatMode == RepeatMode.One ||
                current.timeline?.isAwaitingSeek == true
        )
            return
        val state = current.player.state.value
        if (!state.isPlaying || state.isBuffering || state.mediaStatus != KMMediaStatus.Ready)
            return
        val next = session.nextEntryId() ?: return
        if (
            current.entry?.media?.kind != MediaKind.Audio ||
                session.mediaFor(next)?.kind != MediaKind.Audio
        )
            return
        if (current.entry?.media?.id == session.mediaFor(next)?.id) return
        val standby =
            decks.firstOrNull { it !== current && it.entry?.id == next && it.ready() } ?: return
        val remaining = state.duration?.minus(state.position)?.inWholeMilliseconds ?: return
        autoStartJob =
            scope.launch {
                delay(
                    ((remaining - CrossfadeDurationMs).coerceAtLeast(0L) /
                            state.speed.coerceAtLeast(0.01))
                        .toLong()
                        .milliseconds
                )
                if (
                    selected === current &&
                        session.isPlaying &&
                        incoming == null &&
                        current.ready() &&
                        current.player.state.value.isPlaying &&
                        standby.ready() &&
                        session.nextEntryId() == next &&
                        current.timeline?.isAwaitingSeek != true
                )
                    startAutomaticTransition(current, next)
            }
    }

    private fun startAutomaticTransition(outgoing: Deck, initialId: Int) {
        if (session.radioEnabled) session.selectRadioNext(session.currentEntryId)
        val id = session.nextEntryId() ?: initialId
        val entry = session.entry(id) ?: return
        val standby = decks.firstOrNull { it !== outgoing }
        if (standby == null) {
            session.selectEntry(id, automatic = true)
            select(id)
            return
        }
        autoStartJob?.cancel()
        autoStartJob = null
        preloadStartJob?.cancel()
        preloadStartJob = null
        val previousPreload = preloadJob
        previousPreload?.cancel()
        preloadJob = null
        preloadEntry = null
        incoming = standby
        transitionJob =
            scope.launch {
                try {
                    previousPreload?.join()
                    standby.gain = 0f
                    applyVolumes()
                    prepare(standby, entry)
                    restorePosition(standby)
                    if (completedEntries.remove(entry.id)) standby.timeline?.resetPosition()
                    standby.handledEnd = false
                    standby.setPlaying(session.isPlaying)
                    if (outgoing.player.state.value.mediaStatus == KMMediaStatus.Ended)
                        finishTransition(outgoing, standby)
                    else {
                        fade = Fade()
                        runFade(outgoing, standby)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    cancelAutomaticTransition()
                    session.reportError("Could not transition to the next item", error, id)
                    if (outgoing.player.state.value.mediaStatus == KMMediaStatus.Ended) {
                        outgoing.handledEnd = false
                        session.pause()
                    }
                }
            }
    }

    private fun runFade(outgoing: Deck, standby: Deck) {
        fadeJob?.cancel()
        fadeJob =
            scope.launch {
                combine(
                        snapshotFlow { session.isPlaying },
                        outgoing.player.state,
                        standby.player.state,
                    ) { playing, old, next ->
                        playing &&
                            old.isPlaying &&
                            !old.isBuffering &&
                            next.isPlaying &&
                            !next.isBuffering
                    }
                    .distinctUntilChanged()
                    .collectLatest { playing ->
                        if (!playing) return@collectLatest
                        var previous = TimeSource.Monotonic.markNow()
                        while (this@launch.isActive && incoming === standby && fade != null) {
                            delay(16L.milliseconds)
                            val transition = fade ?: return@collectLatest
                            transition.elapsedMs +=
                                previous.elapsedNow().inWholeNanoseconds / 1_000_000.0
                            previous = TimeSource.Monotonic.markNow()
                            val progress =
                                (transition.elapsedMs / CrossfadeDurationMs).coerceIn(0.0, 1.0)
                            standby.gain = sin(progress * PI / 2).toFloat()
                            outgoing.gain = cos(progress * PI / 2).toFloat()
                            applyVolumes()
                            publishBass()
                            if (progress >= 1.0) return@collectLatest
                        }
                    }
            }
    }

    private fun finishTransition(outgoing: Deck, standby: Deck) {
        val entry = standby.entry ?: return
        outgoing.timeline?.markComplete()
        val outgoingEntry = requireNotNull(outgoing.entry)
        completedEntries += outgoingEntry.id
        session.saveResumePosition(outgoingEntry.media.id, 0f)
        savePosition(standby)
        fadeJob?.cancel()
        fadeJob = null
        fade = null
        incoming = null
        selected = standby
        requestedEntry = entry.id
        standby.published = true
        standby.gain = 1f
        standby.timeline?.beginSelection()
        outgoing.stop()
        session.selectEntry(entry.id, automatic = true)
        standby.timeline?.updateBackend(standby.player.state.value)
        applyVolumes()
        scheduleNextPreload()
        scheduleAutomaticTransition()
    }

    private fun cancelAutomaticTransition() {
        preloadStartJob?.cancel()
        preloadStartJob = null
        autoStartJob?.cancel()
        autoStartJob = null
        transitionJob?.cancel()
        transitionJob = null
        fadeJob?.cancel()
        fadeJob = null
        incoming?.let {
            savePosition(it)
            runCatching { it.stop() }
                .onFailure { error ->
                    session.reportError("Could not stop the transition", error, it.entry?.id)
                }
        }
        incoming = null
        fade = null
        selected?.gain = 1f
        applyVolumes()
    }

    private fun publishBass() {
        bassLevel.floatValue =
            if (!session.isPlaying) 0f
            else
                decks
                    .sumOf { deck ->
                        ((deck.analyzer?.frames?.value?.bandEnergy(40f..160f) ?: 0f) * deck.gain)
                            .toDouble()
                    }
                    .toFloat()
                    .coerceIn(0f, 1f)
    }

    private fun fail(title: String, error: Throwable, id: Int?) {
        failed = true
        requestedEntry = null
        loading.value = false
        cancelAutomaticTransition()
        decks.forEach {
            it.published = false
            it.gain = 0f
            runCatching { it.setPlaying(false) }
        }
        applyVolumes()
        session.pause()
        session.reportError(title, error, id)
    }

    fun clear() {
        ++generation
        closeAll(
            { savePosition(selected) },
            { savePosition(incoming) },
            {
                switchJob?.cancel()
                switchJob = null
                preloadJob?.cancel()
                preloadJob = null
                preloadStartJob?.cancel()
                preloadStartJob = null
                autoStartJob?.cancel()
                autoStartJob = null
                transitionJob?.cancel()
                transitionJob = null
                fadeJob?.cancel()
                fadeJob = null
                incoming = null
                fade = null
                requestedEntry = null
                preloadEntry = null
                selected = null
                loading.value = false
                bassLevel.floatValue = 0f
                failed = false
                completedEntries.clear()
                failedPreloads.clear()
                val decksToClose = decks
                decks = emptyList()
                decksToClose.forEach { deck ->
                    runCatching { deck.close() }
                        .onFailure {
                            session.reportError(
                                "Could not release the media player",
                                it,
                                deck.entry?.id,
                            )
                        }
                }
            },
        )
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            clear()
        } finally {
            seekObservation.cancel()
        }
    }
}
