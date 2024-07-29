package dev.brahmkshatriya.echo.ui.player

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import kotlinx.coroutines.launch

class PlayerUiListener(
    val player: Player,
    val viewModel: PlayerViewModel
) : Player.Listener {

    init {
        updateList()
        with(viewModel) {
            isPlaying.value = player.isPlaying
            buffering.value = player.playbackState == Player.STATE_BUFFERING
            shuffleMode.value = player.shuffleModeEnabled
            repeatMode.value = player.repeatMode
        }
        updateNavigation()
    }

    private fun updateList() = viewModel.run {
        list = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
        viewModelScope.launch {
            listUpdateFlow.emit(Unit)
        }
    }

    private fun updateNavigation() {
        viewModel.nextEnabled.value = player.hasNextMediaItem()
        viewModel.previousEnabled.value =  player.currentMediaItemIndex >= 0
    }

    private val delay = 500L
    private val threshold = 0.2f
    private val updateProgressRunnable = Runnable { updateProgress() }
    private val handler = Handler(Looper.getMainLooper()).also {
        it.post(updateProgressRunnable)
    }
    private fun updateProgress() {
        viewModel.progress.value =
            player.currentPosition.toInt() to player.bufferedPosition.toInt()
        viewModel.totalDuration.value = player.duration.toInt()

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
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        updateList()
        updateNavigation()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        viewModel.shuffleMode.value = shuffleModeEnabled
        updateList()
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNavigation()
        viewModel.repeatMode.value = repeatMode
    }

    override fun onPlayerError(error: PlaybackException) {
        viewModel.createException(error)
    }
}