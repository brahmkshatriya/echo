package dev.brahmkshatriya.echo.player

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import dev.brahmkshatriya.echo.common.models.Track


class PlayerListener(
    private val player: MediaController,
    private val viewModel: PlayerUIViewModel
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

    companion object {
        val tracks = mutableMapOf<String, Track>()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val id = mediaItem?.mediaId ?: return
        val track = mediaItem.localConfiguration?.tag as Track?
        if (track != null) tracks[id] = track
        viewModel.track.value = tracks[id] ?: return
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int
    ) {
        updateNavigation()
        updateProgress()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        updateNavigation()
        updateProgress()
    }

    private fun updateProgress() {
        viewModel.progress.value = player.currentPosition.toInt() to player.bufferedPosition.toInt()
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
    }

    private fun updateNavigation() {
        val index = player.currentMediaItemIndex
        val enablePrevious =  index >= 0
        val enableNext = player.hasNextMediaItem()
        viewModel.nextEnabled.value = enableNext
        viewModel.previousEnabled.value = enablePrevious
    }

    fun update(mediaId: String){
        viewModel.track.value = tracks[mediaId]
        viewModel.totalDuration.value = player.duration.toInt()
        viewModel.isPlaying.value = player.isPlaying
        viewModel.buffering.value = player.playbackState == Player.STATE_BUFFERING
        updateNavigation()
        updateProgress()
    }
}
