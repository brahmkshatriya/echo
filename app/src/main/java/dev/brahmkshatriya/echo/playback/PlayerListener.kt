package dev.brahmkshatriya.echo.playback

import androidx.annotation.CallSuper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded

open class PlayerListener(val player: Player) : Player.Listener {
    open fun onTrackStart(mediaItem: MediaItem) {}
    open fun onTrackEnd(mediaItem: MediaItem) {}

    private var current: MediaItem? = null
    private var loaded = false
    private var playing = false

    @CallSuper
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        current?.let {
            if (!it.isLoaded) return@let
            onTrackEnd(it)
        }
        current = mediaItem
        loaded = mediaItem?.isLoaded ?: false

        if (!loaded || !player.playWhenReady) return
        playing = true
        onTrackStart(mediaItem!!)
    }

    @CallSuper
    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        if (loaded) return
        val mediaItem = player.currentMediaItem ?: return
        if (!mediaItem.isLoaded) return

        current = mediaItem
        loaded = true

        if (!player.playWhenReady) return
        playing = true
        onTrackStart(mediaItem)
    }

    @CallSuper
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (playing) return
        if (!isPlaying) return
        if (!loaded) return
        val mediaItem = player.currentMediaItem ?: return

        playing = true
        onTrackStart(mediaItem)
    }

    @CallSuper
    @UnstableApi
    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        if (reason == 2) return
        val mediaItem = newPosition.mediaItem ?: return
        if (oldPosition.mediaItem != mediaItem) return
        if (newPosition.positionMs != 0L) return

        onTrackEnd(mediaItem)
        onTrackStart(mediaItem)
    }
}