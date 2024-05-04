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
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.di.TrackerModule
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.AUTO_START_RADIO
import dev.brahmkshatriya.echo.utils.tryWith
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.radioNotSupported
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlayerListener(
    private val context: Context,
    private val extensionListFlow: ExtensionModule.ExtensionListFlow,
    private val trackerListFlow: TrackerModule.TrackerListFlow,
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
        if(!::player.isInitialized) return@with
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
        global.currentIndexFlow.value = index
        viewModel?.totalDuration?.value = player.duration.toInt()

        scope.launch {
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
            if (autoStartRadio) global.current?.let {
                scope.launch(Dispatchers.IO) {
                    val client = extensionListFlow.getClient(it.clientId)
                    radio(client) { radio(it.loaded ?: it.onLoad.first()) }
                }
            }
        }
        startedPlaying(player.currentMediaItem?.mediaId)
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        updateCurrent()
        updateNavigation()
        updateProgress()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        viewModel?.shuffle?.value = shuffleModeEnabled
    }

    private fun trackMedia(
        mediaId: String?,
        block: suspend TrackerClient.(clientId: String, track: Track) -> Unit
    ) {
        val streamableTrack = global.getTrack(mediaId) ?: return
        val client = extensionListFlow.getClient(streamableTrack.clientId) ?: return
        val track = streamableTrack.loaded ?: streamableTrack.unloaded
        val clientId = client.metadata.id
        val trackers = trackerListFlow.list ?: emptyList()
        scope.launch(Dispatchers.IO) {
            if (client is TrackerClient) tryWith { client.block(clientId, track) }
            trackers.map {
                launch { tryWith { it.block(clientId, track) } }
            }
        }
    }

    private fun startedPlaying(mediaId: String?) {
        trackMedia(mediaId) { clientId, loaded ->
            onStartedPlaying(clientId, loaded)
        }
    }

    private fun markedAsPlayed(mediaId: String?) {
        trackMedia(mediaId) { clientId, loaded ->
            onMarkAsPlayed(clientId, loaded)
        }
    }

    private val markAsPlayedTime = 10000L // 10 seconds
    private var markedAsPlayed = false
    private fun updateProgress() {
        viewModel?.progress?.value =
            player.currentPosition.toInt() to player.bufferedPosition.toInt()

        if (player.currentPosition <= 0) markedAsPlayed = false
        val current = player.currentMediaItem
        if (current != null) {
            global.saveCurrentPos(context, player.currentPosition)
            if (player.currentPosition > markAsPlayedTime && !markedAsPlayed) {
                markedAsPlayed(current.mediaId)
                markedAsPlayed = true
            }
        }

        handler.removeCallbacks(updateProgressRunnable)
        val playbackState = player.playbackState
        if (playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_ENDED) {
            var delayMs: Long
            if (player.playWhenReady && playbackState == ExoPlayer.STATE_READY) {
                delayMs = 1000 - player.currentPosition % 1000
                if (delayMs < 200) {
                    delayMs += 1000
                }
            } else {
                delayMs = 1000
            }
            handler.postDelayed(updateProgressRunnable, delayMs)
        }
    }

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

    suspend fun radio(
        client: ExtensionClient?,
        block: suspend RadioClient.() -> Playlist
    ) = when (client) {
        null -> {
            messageFlow.emit(context.noClient()); null
        }

        !is RadioClient -> {
            messageFlow.emit(context.radioNotSupported(client.metadata.name)); null
        }

        else -> {
            val tracks = tryWith { block(client) }?.tracks
            tracks?.let { global.addTracks(client.metadata.id, it.toList()).first }
        }
    }


}