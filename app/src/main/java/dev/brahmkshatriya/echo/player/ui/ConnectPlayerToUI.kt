package dev.brahmkshatriya.echo.player.ui

import androidx.activity.viewModels
import androidx.media3.session.MediaBrowser
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.player.PlayerViewModel
import dev.brahmkshatriya.echo.utils.observe

fun connectPlayerToUI(activity: MainActivity, player: MediaBrowser) {

    val uiViewModel: PlayerUIViewModel by activity.viewModels()
    val playerViewModel: PlayerViewModel by activity.viewModels()

    val listener = PlayerListener(player, uiViewModel)
    player.addListener(listener)
    player.currentMediaItem?.let {
        listener.update(it.mediaId)
    }

    activity.apply {
        observe(playerViewModel.playPause) {
            if (it) player.play() else player.pause()
        }
        observe(playerViewModel.seekToPrevious) {
            player.seekToPrevious()
            player.playWhenReady = true
        }
        observe(playerViewModel.seekToNext) {
            player.seekToNext()
            player.playWhenReady = true
        }
        observe(playerViewModel.audioIndexFlow) {
            if (it >= 0) {
                player.seekToDefaultPosition(it)
                uiViewModel.playlist.emit(it)
            }
        }
        observe(playerViewModel.seekTo) {
            player.seekTo(it)
        }
        observe(playerViewModel.repeat) {
            player.repeatMode = it
        }
        observe(playerViewModel.audioQueueFlow) {
            player.addMediaItem(it)
            player.prepare()
            player.playWhenReady = true
        }
        observe(playerViewModel.clearQueueFlow) {
            player.pause()
            player.clearMediaItems()
            player.stop()
        }
        observe(playerViewModel.itemMovedFlow) { (new, old) ->
            player.moveMediaItem(old, new)
        }
        observe(playerViewModel.itemRemovedFlow) {
            player.removeMediaItem(it)
        }
    }
}