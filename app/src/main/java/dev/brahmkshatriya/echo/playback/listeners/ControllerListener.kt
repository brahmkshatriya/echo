package dev.brahmkshatriya.echo.playback.listeners

import android.app.Service
import android.content.Context
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import dev.brahmkshatriya.echo.common.ControllerExtension
import dev.brahmkshatriya.echo.common.clients.ControllerClient
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
class ControllerListener(
    player: Player,
    private val service: Service,
    private val scope: CoroutineScope,
    private val controllerExtensions: MutableStateFlow<List<ControllerExtension>?>,
    private val throwableFlow: MutableSharedFlow<Throwable>
) : PlayerListener(player) {
    private var audioManager: AudioManager = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager

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
                tryOnMain {
                    player.play()
                }
            }
            onPauseRequest = {
                tryOnMain {
                    player.pause()
                }
            }
            onNextRequest = {
                tryOnMain {
                    player.seekToNextMediaItem()
                }
            }
            onPreviousRequest = {
                tryOnMain {
                    player.seekToPreviousMediaItem()
                }
            }
            onSeekRequest = { position ->
                tryOnMain {
                    player.seekTo(position.toLong())
                }
            }
            onMovePlaylistItemRequest = { fromIndex, toIndex ->
                tryOnMain {
                    player.moveMediaItem(fromIndex, toIndex)
                }
            }
            onRemovePlaylistItemRequest = { index ->
                tryOnMain {
                    player.removeMediaItem(index)
                }
            }
            onShuffleModeRequest = { enabled ->
                tryOnMain {
                    player.shuffleModeEnabled = enabled
                }
            }
            onRepeatModeRequest = { repeatMode ->
                tryOnMain {
                    player.repeatMode = repeatMode
                }
            }
            onVolumeRequest = { volume ->
                tryOnMain {
                    val denormalized =
                        volume * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, denormalized.toInt(), 0)
                }
            }
        }
    }

    @Suppress("DEPRECATION") // not being used in startForeground
    private fun canRunCommand(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            service.foregroundServiceType != ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
        } else {
            true //if we are on Android 12 or lower, we can assume it's foreground or can be started as foreground
        }
    }

    private suspend fun ControllerClient.tryOnMain(
        block: suspend ControllerClient.() -> Unit
    ) {
        withContext(Dispatchers.Main.immediate) {
            try {
                if (player.playbackState == Player.STATE_IDLE ||
                    player.playbackState == Player.STATE_ENDED ||
                    (!canRunCommand() && !player.isPlaying)) {
                    return@withContext
                }
                block()
            } catch (e: Exception) {
                throwableFlow.emit(e)
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

    private fun updatePlaylist() {
        val playlist = List(player.mediaItemCount) { index ->
            player.getMediaItemAt(index).track
        }

        notifyControllers {
            onPlaylistChanged(playlist)
        }
    }

    private fun getVolume(): Double {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return volume.toDouble() / maxVolume.toDouble()
    }


    override fun onRenderedFirstFrame() {
        super.onRenderedFirstFrame()
        notifyControllers {
            onVolumeChanged(getVolume())
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
        updatePlaylist()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        super.onTimelineChanged(timeline, reason)
        updatePlaylist()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
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

    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
        super.onDeviceVolumeChanged(volume, muted)
        notifyControllers {
            onVolumeChanged(getVolume())
        }
    }
}