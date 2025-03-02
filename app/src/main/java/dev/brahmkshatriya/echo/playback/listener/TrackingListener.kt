package dev.brahmkshatriya.echo.playback.listener

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.TrackerExtension
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.models.TrackDetails
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.playback.MediaItemUtils.context
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.utils.PauseCountDown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
class TrackingListener(
    private val player: Player,
    private val scope: CoroutineScope,
    private val extensionList: MutableStateFlow<List<MusicExtension>?>,
    private val trackerList: MutableStateFlow<List<TrackerExtension>?>,
    private val throwableFlow: MutableSharedFlow<Throwable>
) : Player.Listener {

    private var current: MediaItem? = null

    private fun trackMedia(
        block: suspend TrackerClient.(extension: Extension<*>, details: TrackDetails?) -> Unit
    ) {
        val details = current?.let {
            TrackDetails(it.extensionId, it.track, it.context)
        }
        scope.launch {
            val extension = extensionList.getExtension(details?.extensionId)
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

    private val timers = mutableMapOf<String, PauseCountDown>()
    private fun onTrackStart(mediaItem: MediaItem?) {
        current = mediaItem
        val isPlaying = player.isPlaying
        timers.forEach { (_, timer) -> timer.pause() }
        timers.clear()
        trackMedia { extension, it ->
            onTrackChanged(it)
            it ?: return@trackMedia
            onPlayingStateChanged(it, isPlaying)
            val duration = markAsPlayedDuration ?: return@trackMedia
            val timer = object : PauseCountDown(duration) {
                override fun onTimerTick(millisUntilFinished: Long) {}
                override fun onTimerFinish() {
                    scope.launch {
                        extension.get<TrackerClient, Unit>(throwableFlow) { onMarkAsPlayed(it) }
                    }
                }
            }
            timers[extension.id] = timer
            withContext(Dispatchers.Main) { timer.start() }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        timers.forEach { (_, timer) ->
            if (isPlaying) timer.start()
            else timer.pause()
        }
        trackMedia { _, it ->
            it ?: return@trackMedia
            onPlayingStateChanged(it, isPlaying)
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (mediaItem?.isLoaded?.not() == true) return
        if (current?.mediaId == mediaItem?.mediaId) return
        onTrackStart(mediaItem)
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        val mediaItem = player.currentMediaItem ?: return
        if (mediaItem.track.id != mediaMetadata.extras.track.id) return
        if (current?.mediaId == mediaItem.mediaId) return

        if (mediaItem.isLoaded) onTrackStart(mediaItem)
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        if (reason == 2) return
        val mediaItem = newPosition.mediaItem ?: return
        if (oldPosition.mediaItem != mediaItem) return
        if (newPosition.positionMs != 0L) return

        onTrackStart(mediaItem)
    }

    override fun onPlayerError(error: PlaybackException) {
        onTrackStart(null)
    }
}