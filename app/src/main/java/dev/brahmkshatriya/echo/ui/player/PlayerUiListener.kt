package dev.brahmkshatriya.echo.ui.player

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C.TIME_UNSET
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.launch

class PlayerUiListener(
    private val player: Player,
    private val viewModel: PlayerViewModel
) : Player.Listener {

    init {
        updateList()
        with(viewModel) {
            tracks.value = player.currentTracks
            isPlaying.value = player.isPlaying
            buffering.value = player.playbackState == Player.STATE_BUFFERING
            shuffleMode.value = player.shuffleModeEnabled
            repeatMode.value = player.repeatMode
        }
        updateNavigation()
    }

    private fun updateList() = viewModel.run {
        updateNavigation()
        queue = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
        viewModelScope.launch { queueFlow.emit(Unit) }
    }

    private fun updateNavigation() {
        viewModel.nextEnabled.value = player.hasNextMediaItem()
        viewModel.previousEnabled.value = player.currentMediaItemIndex >= 0
    }

    private val delay = 500L
    private val threshold = 0.2f
    private val updateProgressRunnable = Runnable { updateProgress() }
    private val handler = Handler(Looper.getMainLooper()).also {
        it.post(updateProgressRunnable)
    }

    private fun updateProgress() {
        viewModel.progress.value =
            player.currentPosition to player.bufferedPosition
        viewModel.totalDuration.value = player.duration.takeIf { it != TIME_UNSET }

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

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING ->
                viewModel.buffering.value = true

            Player.STATE_READY -> {
                viewModel.buffering.value = false
            }

            else -> Unit
        }
        updateProgress()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        viewModel.isPlaying.value = isPlaying
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int
    ) {
        updateNavigation()
        updateProgress()
        viewModel.discontinuity.value = newPosition.positionMs
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        updateList()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        viewModel.shuffleMode.value = player.shuffleModeEnabled
        updateList()
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        viewModel.repeatMode.value = player.repeatMode
        updateList()
    }

    override fun onPlayerError(error: PlaybackException) {
        viewModel.isPlaying.value = false
        viewModel.buffering.value = false
    }

    override fun onTracksChanged(tracks: Tracks) {
        viewModel.tracks.value = tracks
    }
}