package dev.brahmkshatriya.echo.playback.listener

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.models.TrackDetails
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.extensions.Extensions
import dev.brahmkshatriya.echo.playback.MediaItemUtils.context
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.utils.PauseTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    extensions: Extensions,
    private val currentFlow: MutableStateFlow<PlayerState.Current?>,
    private val throwableFlow: MutableSharedFlow<Throwable>
) : Player.Listener {

    private val musicList = extensions.music
    private val trackerList = extensions.tracker

    private var current: MediaItem? = null

    private fun trackMedia(
        block: suspend TrackerClient.(extension: Extension<*>, details: TrackDetails?) -> Unit
    ) {
        val details = current?.let {
            TrackDetails(it.extensionId, it.track, it.context)
        }
        scope.launch {
            val extension = musicList.getExtension(details?.extensionId)
            val trackers = trackerList.value?.filter { it.isEnabled } ?: emptyList()
            extension?.get<TrackerClient, Unit>(throwableFlow) {
                block(extension, details)
            }
            trackers.forEach {
                launch {
                    it.get<TrackerClient, Unit>(throwableFlow) {
                        block(it, details)
                    }
                }
            }
        }
    }

    private val mutex = Mutex()
    private val timers = mutableMapOf<String, PauseTimer>()
    private fun onTrackChanged(mediaItem: MediaItem?) {
        current = mediaItem
        trackMedia { extension, details ->
            mutex.withLock {
                timers.forEach { (_, timer) -> timer.pause() }
                timers.clear()
            }
            onTrackChanged(details)
            val isPlaying = withContext(Dispatchers.Main) { player.isPlaying }
            onPlayingStateChanged(details, isPlaying)
            details ?: return@trackMedia
            val duration = markAsPlayedDuration ?: return@trackMedia
            mutex.withLock {
                timers[extension.id] = PauseTimer(scope, duration) {
                    scope.launch {
                        extension.get<TrackerClient, Unit>(throwableFlow) { onMarkAsPlayed(details) }
                    }
                }
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        trackMedia { _, it ->
            mutex.withLock {
                timers.forEach { (_, timer) ->
                    if (isPlaying) timer.resume()
                    else timer.pause()
                }
            }
            onPlayingStateChanged(it, isPlaying)
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int
    ) {
        if (reason == 2) return
        val mediaItem = newPosition.mediaItem ?: return
        if (oldPosition.mediaItem != mediaItem) return
        if (newPosition.positionMs != 0L) return

        onTrackChanged(current)
    }

    override fun onPlayerError(error: PlaybackException) {
        onTrackChanged(null)
    }

    init {
        scope.launch {
            currentFlow.map { it?.let { curr -> curr.mediaItem.takeIf { curr.isLoaded } } }
                .distinctUntilChanged().collectLatest { onTrackChanged(it) }
        }
    }
}