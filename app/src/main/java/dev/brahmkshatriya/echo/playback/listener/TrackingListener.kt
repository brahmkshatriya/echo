package dev.brahmkshatriya.echo.playback.listener

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.models.TrackDetails
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.playback.MediaItemUtils.context
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.utils.PauseTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@UnstableApi
class TrackingListener(
    private val player: Player,
    private val scope: CoroutineScope,
    extensions: ExtensionLoader,
    private val currentFlow: MutableStateFlow<PlayerState.Current?>,
    private val throwableFlow: MutableSharedFlow<Throwable>
) : Player.Listener {

    private val musicList = extensions.music
    private val trackerList = extensions.tracker

    private var current: MediaItem? = null
    private var previousId: String? = null

    private suspend fun getDetails() = withContext(Dispatchers.Main) {
        current?.let { curr ->
            val (pos, total) = player.currentPosition to player.duration.takeIf { it != C.TIME_UNSET }
            TrackDetails(curr.extensionId, curr.track, curr.context, pos, total)
        }
    }

    private fun trackMedia(
        block: suspend TrackerClient.(extension: Extension<*>, details: TrackDetails?) -> Unit
    ) {
        scope.launch {
            val details = getDetails()
            val prevExtension = previousId?.takeIf { details?.extensionId != it }
                ?.let { musicList.getExtension(it) }
            val extension = musicList.getExtension(details?.extensionId)
            val trackers = trackerList.value.filter { it.isEnabled }
            prevExtension?.get<TrackerClient, Unit>(throwableFlow) { block(prevExtension, null) }
            extension?.get<TrackerClient, Unit>(throwableFlow) { block(extension, details) }
            trackers.forEach {
                launch { it.get<TrackerClient, Unit>(throwableFlow) { block(it, details) } }
            }
        }
    }

    private val mutex = Mutex()
    private val timers = mutableMapOf<String, PauseTimer>()
    private fun onTrackChanged(mediaItem: MediaItem?) {
        previousId = current?.extensionId
        current = mediaItem
        scope.launch {
            mutex.withLock {
                timers.forEach { (_, timer) -> timer.pause() }
                timers.clear()
            }
            trackMedia { extension, details ->
                onTrackChanged(details)
                details ?: return@trackMedia
                val duration = markAsPlayedDuration ?: return@trackMedia
                mutex.withLock {
                    timers[extension.id] = PauseTimer(scope, duration) {
                        scope.launch {
                            extension.get<TrackerClient, Unit>(throwableFlow) {
                                onMarkAsPlayed(details)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        scope.launch {
            playState.value = getDetails() to isPlaying
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int
    ) {
        scope.launch {
            val isPlaying = withContext(Dispatchers.Main) { player.isPlaying }
            playState.value = getDetails() to isPlaying
        }
        if (reason == 2 || current == null) return
        val mediaItem = newPosition.mediaItem ?: return
        if (oldPosition.mediaItem != mediaItem) return
        if (newPosition.positionMs != 0L) return

        onTrackChanged(current)
    }

    override fun onPlayerError(error: PlaybackException) {
        onTrackChanged(null)
    }

    private val playState = MutableStateFlow<Pair<TrackDetails?, Boolean>>(null to false)

    init {
        scope.launch {
            currentFlow.map { it?.let { curr -> curr.mediaItem.takeIf { curr.isLoaded } } }
                .distinctUntilChanged().collectLatest {
                    onTrackChanged(it)
                }
        }
        scope.launch {
            @OptIn(FlowPreview::class)
            playState.debounce(500).collectLatest { (_, isPlaying) ->
                mutex.withLock {
                    timers.forEach { (_, timer) ->
                        if (isPlaying) timer.resume()
                        else timer.pause()
                    }
                }
                trackMedia { _, details ->
                    onPlayingStateChanged(details, isPlaying)
                }
            }
        }
    }
}