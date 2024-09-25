package dev.brahmkshatriya.echo.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.TrackerExtension
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.context
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.utils.PauseCountDown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@UnstableApi
class TrackingListener(
    session: MediaLibrarySession,
    private val scope: CoroutineScope,
    private val extensionList: MutableStateFlow<List<MusicExtension>?>,
    private val trackerList: MutableStateFlow<List<TrackerExtension>?>,
    private val throwableFlow: MutableSharedFlow<Throwable>
) : PlayerListener(session.player) {

    private var current: MediaItem? = null
    private var timer: PauseCountDown? = null

    private fun trackMedia(
        block: suspend TrackerClient.(clientId: String, context: EchoMediaItem?, track: Track) -> Unit
    ) {
        val item = current ?: return
        val clientId = item.clientId
        val track = item.track

        val extension = extensionList.getExtension(clientId) ?: return
        val trackers = trackerList.value?.filter { it.metadata.enabled } ?: emptyList()

        scope.launch(Dispatchers.IO) {
            extension.get<TrackerClient, Unit>(throwableFlow) {
                block(clientId, item.context, track)
            }
            trackers.forEach {
                launch {
                    it.get<TrackerClient, Unit>(throwableFlow) {
                        block(clientId, item.context, track)
                    }
                }
            }
        }
    }


    override fun onTrackStart(mediaItem: MediaItem) {
        current = mediaItem
        trackMedia(TrackerClient::onStartedPlaying)
        timer = object : PauseCountDown(30000) {
            override fun onTimerTick(millisUntilFinished: Long) {
                // Can be Implemented
            }

            override fun onTimerFinish() {
                trackMedia(TrackerClient::onMarkAsPlayed)
            }
        }
        timer?.start()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        // Can be implemented
        if (isPlaying) timer?.start()
        else timer?.pause()
    }

    override fun onTrackEnd(mediaItem: MediaItem) {
        timer?.pause()
        timer = null
        trackMedia(TrackerClient::onStoppedPlaying)
        current = null
    }
}