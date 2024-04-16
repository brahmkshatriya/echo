package dev.brahmkshatriya.echo.player

import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaBrowser
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel

class PlayerListener(
    private val player: MediaBrowser, private val viewModel: PlayerViewModel
) : Player.Listener {

    private val updateProgressRunnable = Runnable { updateProgress() }
    private val handler = Handler(Looper.getMainLooper()).also {
        it.post(updateProgressRunnable)
    }

    init {
        viewModel.isPlaying.value = player.isPlaying
        viewModel.buffering.value = player.playbackState == Player.STATE_BUFFERING
        viewModel.shuffle.value = player.shuffleModeEnabled
        viewModel.repeat.value = player.repeatMode
        updateCurrent()
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

    private fun updateCurrent() {
        val mediaItems = (0 until player.mediaItemCount).map {
            player.getMediaItemAt(it).mediaId
        }
        viewModel.updateList(mediaItems, player.currentMediaItemIndex)
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int
    ) {
        updateNavigation()
        updateProgress()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        updateCurrent()
        viewModel.startedPlaying(player.currentMediaItem?.mediaId)
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        updateCurrent()
        updateNavigation()
        updateProgress()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        viewModel.shuffle.value = shuffleModeEnabled
    }

    private val markAsPlayedTime = 10000L // 10 seconds
    private var previousMediaId: String? = null
    private fun updateProgress() {
        if (player.isConnected) {
            viewModel.progress.value =
                player.currentPosition.toInt() to player.bufferedPosition.toInt()
            viewModel.totalDuration.value = player.duration.toInt()
            if (player.currentPosition > markAsPlayedTime) {
                val mediaId = player.currentMediaItem?.mediaId
                if (mediaId != previousMediaId) {
                    previousMediaId = mediaId
                    viewModel.markedAsPlayed(mediaId)
                }
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
        viewModel.repeat.value = repeatMode
    }

    private fun updateNavigation() {
        val index = player.currentMediaItemIndex
        val enablePrevious = index >= 0
        val enableNext = player.hasNextMediaItem()
        viewModel.nextEnabled.value = enableNext
        viewModel.previousEnabled.value = enablePrevious
    }

    override fun onPlayerError(error: PlaybackException) {
        viewModel.createException(error)
    }

}