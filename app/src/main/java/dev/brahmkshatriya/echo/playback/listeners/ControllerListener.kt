package dev.brahmkshatriya.echo.playback.listeners

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import dev.brahmkshatriya.echo.common.ControllerExtension
import dev.brahmkshatriya.echo.common.clients.ControllerClient
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@UnstableApi
class ControllerListener(
    player: Player,
    private val scope: CoroutineScope,
    private val controllerExtensions: MutableStateFlow<List<ControllerExtension>?>,
    private val throwableFlow: MutableSharedFlow<Throwable>
) : PlayerListener(player) {

    init {
        scope.launch {
            controllerExtensions.collect { extensions ->
                extensions?.forEach {
                    launch {
                        registerController(it)
                    }
                }
            }
        }
    }

    private suspend fun registerController(extension: ControllerExtension) {
        extension.get<ControllerClient, Unit>(throwableFlow) {
            onPlayRequest = {
                try {
                    player.play()
                } catch (e: Exception) {
                    throwableFlow.emit(e)
                }
            }
            onPauseRequest = {
                try {
                    player.pause()
                } catch (e: Exception) {
                    throwableFlow.emit(e)
                }
            }
            onNextRequest = {
                try {
                    player.seekToNextMediaItem()
                } catch (e: Exception) {
                    throwableFlow.emit(e)
                }
            }
            onPreviousRequest = {
                try {
                    player.seekToPreviousMediaItem()
                } catch (e: Exception) {
                    throwableFlow.emit(e)
                }
            }
            onSeekRequest = { position ->
                try {
                    player.seekTo(position.toLong())
                } catch (e: Exception) {
                    throwableFlow.emit(e)
                }
            }
            onMovePlaylistItemRequest = { fromIndex, toIndex ->
                try {
                    player.moveMediaItem(fromIndex, toIndex)
                } catch (e: Exception) {
                    throwableFlow.emit(e)
                }
            }
            onRemovePlaylistItemRequest = { index ->
                try {
                    player.removeMediaItem(index)
                } catch (e: Exception) {
                    throwableFlow.emit(e)
                }
            }
            onShuffleModeRequest = { enabled ->
                try {
                    player.shuffleModeEnabled = enabled
                } catch (e: Exception) {
                    throwableFlow.emit(e)
                }
            }
            onRepeatModeRequest = { repeatMode ->
                try {
                    player.repeatMode = repeatMode
                } catch (e: Exception) {
                    throwableFlow.emit(e)
                }
            }
            onVolumeRequest = { volume ->
                try {
                    player.volume = volume.toFloat()
                } catch (e: Exception) {
                    throwableFlow.emit(e)
                }
            }
        }

    }

    private fun notifyControllers(block: suspend ControllerClient.() -> Unit) {
        val controllers = controllerExtensions.value?.filter { it.metadata.enabled } ?: emptyList()
        scope.launch {
            controllers.forEach {
                launch {
                    it.get<ControllerClient, Unit>(throwableFlow) { block() }
                }
            }
        }
    }

    override fun onTrackStart(mediaItem: MediaItem) {
        val isPlaying = player.isPlaying
        val position = player.currentPosition.toDouble()
        val track = mediaItem.track

        notifyControllers {
            onPlaybackStateChanged(isPlaying, position, track)
        }
    }

    override fun onTracksChanged(tracks: Tracks) {
        super.onTracksChanged(tracks)
        val playlist = List(player.mediaItemCount) { index ->
            player.getMediaItemAt(index).track
        }

        notifyControllers {
            onPlaylistChanged(playlist)
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        val position = player.currentPosition.toDouble()
        val track = player.currentMediaItem?.track ?: return

        notifyControllers {
            onPlaybackStateChanged(isPlaying, position, track)
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        super.onShuffleModeEnabledChanged(shuffleModeEnabled)
        val repeatMode = player.repeatMode

        notifyControllers {
            onPlaybackModeChanged(shuffleModeEnabled, repeatMode)
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        super.onRepeatModeChanged(repeatMode)
        val shuffleModeEnabled = player.shuffleModeEnabled

        notifyControllers {
            onPlaybackModeChanged(shuffleModeEnabled, repeatMode)
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
        val position = newPosition.positionMs.toDouble()

        notifyControllers {
            onPositionChanged(position)
        }
    }

    override fun onVolumeChanged(volume: Float) {
        super.onVolumeChanged(volume)
        notifyControllers {
            onVolumeChanged(player.volume.toDouble())
        }
    }
}