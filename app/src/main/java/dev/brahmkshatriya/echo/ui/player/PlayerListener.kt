package dev.brahmkshatriya.echo.ui.player

import android.annotation.SuppressLint
import android.view.View
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import com.google.android.material.checkbox.MaterialCheckBox
import dev.brahmkshatriya.echo.data.models.Track
import dev.brahmkshatriya.echo.databinding.BottomPlayerBinding
import dev.brahmkshatriya.echo.ui.player.PlayerHelper.Companion.toTimeString
import dev.brahmkshatriya.echo.ui.utils.loadInto


class PlayerListener(
    private val player: MediaController,
    private val binding: BottomPlayerBinding,
    private val playPauseListener: MaterialCheckBox.OnCheckedStateChangedListener
) : Player.Listener {

    private val updateProgressRunnable = Runnable { updateProgress() }

    init {
        val listener = this
        binding.root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(p0: View) {}
            override fun onViewDetachedFromWindow(p0: View) {
                player.removeListener(listener)
            }
        })
        binding.root.post(updateProgressRunnable)
    }

    @SuppressLint("SwitchIntDef")
    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING -> {
                binding.trackPlayPause.isEnabled = false
                binding.collapsedTrackPlayPause.isEnabled = false
            }

            Player.STATE_READY -> {
                binding.trackPlayPause.isEnabled = true
                binding.collapsedTrackPlayPause.isEnabled = true

//                if (player.duration == C.TIME_UNSET) throw IllegalStateException("Duration is not set")

                binding.collapsedSeekBar.isIndeterminate = false
                binding.expandedSeekBar.isEnabled = true

                binding.collapsedSeekBar.max = player.duration.toInt()
                binding.expandedSeekBar.max = player.duration.toInt()

                binding.trackTotalTime.text = player.duration.toTimeString()
            }
        }
        updateProgress()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        binding.trackPlayPause.removeOnCheckedStateChangedListener(playPauseListener)
        binding.collapsedTrackPlayPause.removeOnCheckedStateChangedListener(playPauseListener)

        binding.trackPlayPause.isChecked = isPlaying
        binding.collapsedTrackPlayPause.isChecked = isPlaying

        binding.trackPlayPause.addOnCheckedStateChangedListener(playPauseListener)
        binding.collapsedTrackPlayPause.addOnCheckedStateChangedListener(playPauseListener)

    }

    val map = mutableMapOf<MediaMetadata, Track>()
    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        val track = map[mediaMetadata] ?: return

        binding.collapsedTrackTitle.text = track.title
        binding.expandedTrackTitle.text = track.title

        track.artists.joinToString(" ") { it.name }.run {
            binding.collapsedTrackAuthor.text = this
            binding.expandedTrackAuthor.text = this
        }
        track.cover?.run {
            loadInto(binding.collapsedTrackCover)
            loadInto(binding.expandedTrackCover)
        }

        binding.collapsedSeekBar.isIndeterminate = true
        binding.expandedSeekBar.isEnabled = false

        binding.trackTotalTime.text = null
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
        if (!binding.root.isAttachedToWindow) return

        if (!binding.expandedSeekBar.isPressed) {
            binding.collapsedSeekBar.progress = player.currentPosition.toInt()
            binding.collapsedSeekBar.secondaryProgress = player.bufferedPosition.toInt()

            binding.expandedSeekBar.secondaryProgress = player.bufferedPosition.toInt()
            binding.expandedSeekBar.progress = player.currentPosition.toInt()

            binding.trackCurrentTime.text = player.currentPosition.toTimeString()
        }

        binding.root.removeCallbacks(updateProgressRunnable)
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
            binding.root.postDelayed(updateProgressRunnable, delayMs)
        }

    }

    private fun updateNavigation() {
        if (!binding.root.isAttachedToWindow) return
        val index = player.currentMediaItemIndex
        val enablePrevious = index >= 0
        val enableNext = index < player.mediaItemCount - 1
        binding.trackNext.isEnabled = enableNext
        binding.trackPrevious.isEnabled = enablePrevious
    }
}
