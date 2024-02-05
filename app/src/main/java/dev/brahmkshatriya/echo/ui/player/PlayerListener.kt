package dev.brahmkshatriya.echo.ui.player

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import dev.brahmkshatriya.echo.data.models.Track
import dev.brahmkshatriya.echo.databinding.BottomPlayerBinding
import dev.brahmkshatriya.echo.ui.player.PlayerHelper.Companion.toTimeString
import dev.brahmkshatriya.echo.ui.utils.loadInto

class PlayerListener(val player: MediaController, val binding: BottomPlayerBinding) :
    Player.Listener {
    init {
        //Poll each second to update the seekbar
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                binding.collapsedSeekBar.progress = player.currentPosition.toInt()
                binding.expandedSeekBar.value = player.currentPosition.toFloat()

                binding.trackCurrentTime.text = player.currentPosition.toTimeString()

                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
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
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        binding.trackPlayPause.isEnabled = false
        binding.collapsedTrackPlayPause.isEnabled = false

        binding.trackPlayPause.isChecked = isPlaying
        binding.collapsedTrackPlayPause.isChecked = isPlaying

        binding.trackPlayPause.isEnabled = true
        binding.collapsedTrackPlayPause.isEnabled = true
    }


    private val tracks = mutableMapOf<MediaMetadata, Track>()
    fun map(metadata: MediaMetadata, track: Track) {
        tracks[metadata] = track
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {

        val track = tracks[mediaMetadata] ?: return
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
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        if (reason != Player.DISCONTINUITY_REASON_SEEK && reason != Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
            binding.collapsedSeekBar.progress = newPosition.positionMs.toInt()
            binding.expandedSeekBar.value = newPosition.positionMs.toFloat()

            binding.trackCurrentTime.text = newPosition.positionMs.toTimeString()
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (!playWhenReady) return
        if(player.duration == C.TIME_UNSET) return

        binding.collapsedSeekBar.isIndeterminate = false
        binding.expandedSeekBar.isEnabled = true

        binding.collapsedSeekBar.max = player.duration.toInt()
        binding.expandedSeekBar.valueTo = player.duration.toFloat()

        binding.trackTotalTime.text = player.duration.toTimeString()
    }

    override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {
        super.onPlaylistMetadataChanged(mediaMetadata)
        println(mediaMetadata)
    }
}
