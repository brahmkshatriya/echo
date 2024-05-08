package dev.brahmkshatriya.echo.playback

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.TrackerExtension
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.AUTO_START_RADIO
import dev.brahmkshatriya.echo.utils.tryWith
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.radioNotSupported
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlayerListener(
    private val context: Context,
    private val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    private val trackerListFlow: MutableStateFlow<List<TrackerExtension>?>,
    private val global: Queue,
    private val settings: SharedPreferences,
    private val throwableFlow: MutableSharedFlow<Throwable>,
    private val messageFlow: MutableSharedFlow<SnackBar.Message>
) : Player.Listener {

    private lateinit var player: Player
    private lateinit var scope: CoroutineScope

    fun setup(player: ExoPlayer, scope: CoroutineScope) {
        this.player = player
        this.scope = scope
        player.addListener(this)
        viewModel?.let { setViewModel(it) }
    }


    private var viewModel: PlayerViewModel? = null
    fun setViewModel(playerViewModel: PlayerViewModel) = with(playerViewModel) {
        viewModel = this
        if (!::player.isInitialized) return@with
        isPlaying.value = player.isPlaying
        buffering.value = player.playbackState == Player.STATE_BUFFERING
        shuffle.value = player.shuffleModeEnabled
        repeat.value = player.repeatMode
    }

    private fun updateCurrent() {
        val mediaItems = (0 until player.mediaItemCount).map {
            player.getMediaItemAt(it).mediaId
        }
        global.updateQueue(mediaItems)
        val index = player.currentMediaItemIndex
        global.saveQueue(context, index)
        scope.launch {
            global.currentIndex = index
            global.currentIndexFlow.emit(index)
            viewModel?.totalDuration?.value = player.duration.toInt()
            global.updateFlow.emit(Unit)
        }
    }


    private val updateProgressRunnable = Runnable { updateProgress() }
    private val handler = Handler(Looper.getMainLooper()).also {
        it.post(updateProgressRunnable)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING ->
                viewModel?.buffering?.value = true

            Player.STATE_READY -> {
                viewModel?.buffering?.value = false
            }

            else -> Unit
        }
        updateProgress()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        viewModel?.isPlaying?.value = isPlaying
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int
    ) {
        updateNavigation()
        updateProgress()
    }

    private suspend fun <T> tryWith(block: suspend () -> T) = tryWith(throwableFlow, block)

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        updateCurrent()
        if (!player.hasNextMediaItem()) {
            val autoStartRadio = settings.getBoolean(AUTO_START_RADIO, true)
            if (autoStartRadio) global.current?.let { track ->
                scope.launch(Dispatchers.IO) {
                    val list = extensionListFlow.first { it != null }
                    val extension = list?.find { it.metadata.id == track.clientId }
                    radio(extension) {
                        when (val item = track.context) {
                            is EchoMediaItem.Lists.PlaylistItem -> radio(item.playlist)
                            is EchoMediaItem.Lists.AlbumItem -> radio(item.album)
                            else -> radio(track.loaded ?: track.onLoad.first())
                        }
                    }
                }
            }
        }
        stoppedPlaying()
        startedPlaying(player.currentMediaItem?.mediaId)
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        updateCurrent()
        updateNavigation()
        updateProgress()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        viewModel?.shuffle?.value = shuffleModeEnabled
        val indexes = mutableListOf(0)
        var index = 0
        while(index != -1) {
            index = player.currentTimeline.getNextWindowIndex(index, player.repeatMode, shuffleModeEnabled)
            if (index != -1) indexes.add(index)
        }
    }

    private fun trackMedia(
        mediaId: String?,
        block: suspend TrackerClient.(clientId: String, context: EchoMediaItem?, track: Track) -> Unit
    ) {
        val streamableTrack = global.getTrack(mediaId) ?: return
        val client = extensionListFlow.getExtension(streamableTrack.clientId)?.client ?: return
        val track = streamableTrack.loaded ?: streamableTrack.unloaded
        val clientId = streamableTrack.clientId
        val trackers = trackerListFlow.value ?: emptyList()
        scope.launch(Dispatchers.IO) {
            if (client is TrackerClient) tryWith {
                client.block(clientId, streamableTrack.context, track)
            }
            trackers.map {
                launch {
                    tryWith {
                        it.client.block(clientId, streamableTrack.context, track)
                    }
                }
            }
        }
    }

    private var currentlyPlaying: String? = null
    private fun startedPlaying(mediaId: String?) {
        currentlyPlaying = mediaId
        trackMedia(mediaId) { clientId, context, loaded ->
            onStartedPlaying(clientId, context, loaded)
        }
    }

    private fun markedAsPlayed() {
        val mediaId = currentlyPlaying
        trackMedia(mediaId) { clientId, context, loaded ->
            onMarkAsPlayed(clientId, context, loaded)
        }
    }

    private fun stoppedPlaying() {
        val mediaId = currentlyPlaying
        trackMedia(mediaId) { clientId, context, loaded ->
            onStoppedPlaying(clientId, context, loaded)
        }
    }

    private val markAsPlayedTime = 30 * 1000L // 30 seconds
    private var markedAsPlayed = false
    private fun updateProgress() {
        viewModel?.progress?.value =
            player.currentPosition.toInt() to player.bufferedPosition.toInt()

        if (player.currentPosition <= 0) markedAsPlayed = false
        val current = player.currentMediaItem
        if (current != null) {
            global.saveCurrentPos(context, player.currentPosition)
            if (player.currentPosition > markAsPlayedTime && !markedAsPlayed) {
                markedAsPlayed()
                markedAsPlayed = true
            }
        }

        handler.removeCallbacks(updateProgressRunnable)
        val playbackState = player.playbackState
        if (playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_ENDED) {
            var delayMs: Long
            if (player.playWhenReady && playbackState == ExoPlayer.STATE_READY) {
                delayMs = delay - player.currentPosition % delay
                if (delayMs < delay * threshold) {
                    delayMs += delay
                }
            } else {
                delayMs = delay
            }
            handler.postDelayed(updateProgressRunnable, delayMs)
        }
    }

    private val delay = 500L
    private val threshold = 0.2f

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNavigation()
        viewModel?.repeat?.value = repeatMode
        global.repeat.value = repeatMode
    }

    private fun updateNavigation() {
        val index = player.currentMediaItemIndex
        val enablePrevious = index >= 0
        val enableNext = player.hasNextMediaItem()
        viewModel?.nextEnabled?.value = enableNext
        viewModel?.previousEnabled?.value = enablePrevious
    }

    override fun onPlayerError(error: PlaybackException) {
        viewModel?.createException(error)
    }

    //TODO Continuous Playlist
    suspend fun radio(
        extension: MusicExtension?,
        block: suspend RadioClient.() -> Playlist
    ) = when (val client = extension?.client) {
        null -> {
            messageFlow.emit(context.noClient()); null
        }

        !is RadioClient -> {
            messageFlow.emit(context.radioNotSupported(extension.metadata.name)); null
        }

        else -> {
            val playlist = tryWith { block(client) }
            val tracks = playlist?.let { tryWith { client.loadTracks(it).loadFirst() } }
            tracks?.let {
                global.addTracks(
                    extension.metadata.id,
                    playlist.toMediaItem(),
                    it
                ).first
            }
        }
    }


}