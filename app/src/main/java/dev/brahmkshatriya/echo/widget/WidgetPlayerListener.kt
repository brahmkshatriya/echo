package dev.brahmkshatriya.echo.widget

import android.graphics.Bitmap
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dev.brahmkshatriya.echo.playback.PlayerCommands.imageCommand
import dev.brahmkshatriya.echo.utils.Serializer.getParcel

class WidgetPlayerListener(
    private val update: (Bitmap?) -> Unit
) : Player.Listener {

    var controller: MediaController? = null
    private var result: ListenableFuture<SessionResult>? = null
    private var image: Bitmap? = null
    private fun getImage() {
        result?.cancel(true)
        result = controller?.sendCustomCommand(imageCommand, Bundle.EMPTY)
        result?.addListener({
            val result = result?.get()
            if (result?.resultCode == SessionResult.RESULT_SUCCESS) {
                image = result.extras.getParcel<Bitmap>("image")
                update(image)
            }
        }, MoreExecutors.directExecutor())
    }

    fun removed() {
        result?.cancel(true)
        result = null
        controller?.removeListener(this)
        controller = null
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        getImage()
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        getImage()
        update(image)
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        update(image)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        update(image)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        update(image)
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        update(image)
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        update(image)
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int
    ) {
        update(image)
    }
}