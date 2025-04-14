package dev.brahmkshatriya.echo.widget

import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline

class WidgetPlayerListener(
    private val update: () -> Unit
) : Player.Listener {

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        update()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        update()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        update()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        update()
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        update()
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        update()
    }
}