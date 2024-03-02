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
    listener.update()

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
            }
        }
        observe(playerViewModel.seekTo) {
            player.seekTo(it)
        }
        observe(playerViewModel.repeat) {
            player.repeatMode = it
        }
        observe(playerViewModel.shuffle) {
            player.shuffleModeEnabled = it
        }
        observe(playerViewModel.addTrackFlow) { (index, item) ->
            player.addMediaItem(index, item)
            player.prepare()
            player.playWhenReady = true
        }
        observe(playerViewModel.moveTrackFlow) { (new, old) ->
            player.moveMediaItem(old, new)
        }
        observe(playerViewModel.removeTrackFlow) {
            player.removeMediaItem(it)
        }
        observe(playerViewModel.clearQueueFlow) {
            if(player.mediaItemCount == 0) return@observe
            player.pause()
            player.clearMediaItems()
            player.stop()
        }
    }
}