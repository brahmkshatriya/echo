package dev.brahmkshatriya.echo.player.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaBrowser
import dev.brahmkshatriya.echo.common.models.Track

class PlayerListener(
    private val player: MediaBrowser, private val viewModel: PlayerUIViewModel
) : Player.Listener {

    private val updateProgressRunnable = Runnable { updateProgress() }
    private val handler = Handler(Looper.getMainLooper()).also {
        it.post(updateProgressRunnable)
    }

    @SuppressLint("SwitchIntDef")
    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING -> {
                viewModel.buffering.value = true
            }

            Player.STATE_READY -> {
                viewModel.buffering.value = false
                viewModel.totalDuration.value = player.duration.toInt()
            }
        }
        updateProgress()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        viewModel.isPlaying.value = isPlaying
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        viewModel.track.value = viewModel.getTrack(mediaItem?.mediaId)
        viewModel.changeCurrent(player.currentMediaItemIndex.let { if (it == C.INDEX_UNSET) null else it })
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int
    ) {
        updateNavigation()
        updateProgress()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (player.currentMediaItem == null) {
            viewModel.track.value = null
            viewModel.changeCurrent(null)
        }
        updateNavigation()
        updateProgress()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        viewModel.shuffled.value = shuffleModeEnabled
    }

    private var lastRadioTrack : Track?= null
    private fun updateProgress() {
        if (player.isConnected) {
            viewModel.progress.value =
                player.currentPosition.toInt() to player.bufferedPosition.toInt()
            // radio if last item and song is 80% done
            val percentage = player.currentPosition * 1f / player.duration
            if (!player.hasNextMediaItem() && percentage > 0.8f) {
                if (lastRadioTrack != viewModel.track.value) {
                    lastRadioTrack = viewModel.track.value
                    viewModel.radio()
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
        viewModel.repeatMode = repeatMode
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

    fun update() {
        viewModel.track.value = viewModel.getTrack(player.currentMediaItem?.mediaId)
        viewModel.totalDuration.value = player.duration.toInt()
        viewModel.isPlaying.value = player.isPlaying
        viewModel.buffering.value = player.playbackState == Player.STATE_BUFFERING
        viewModel.changeCurrent(player.currentMediaItemIndex.let { if (it == C.INDEX_UNSET) null else it })
        viewModel.shuffled.value = player.shuffleModeEnabled
    }
}