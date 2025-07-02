package dev.brahmkshatriya.echo.playback.listener

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaSession
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.retries
import dev.brahmkshatriya.echo.playback.PlayerCommands.getLikeButton
import dev.brahmkshatriya.echo.playback.PlayerCommands.getRepeatButton
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.ResumptionUtils
import dev.brahmkshatriya.echo.playback.exceptions.PlayerException
import dev.brahmkshatriya.echo.utils.Serializer.rootCause
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

class PlayerEventListener(
    private val context: Context,
    private val scope: CoroutineScope,
    private val session: MediaSession,
    private val currentFlow: MutableStateFlow<PlayerState.Current?>,
    private val extensions: ExtensionLoader,
    private val throwableFlow: MutableSharedFlow<Throwable>
) : Player.Listener {

    private val player get() = session.player

    private fun updateCustomLayout() = scope.launch(Dispatchers.Main) {
        val item = player.currentMediaItem ?: return@launch
        val supportsLike = withContext(Dispatchers.IO) {
            extensions.music.getExtension(item.extensionId)?.isClient<TrackLikeClient>() ?: false
        }
        val commandButtons = listOfNotNull(
            getRepeatButton(context, player.repeatMode),
            getLikeButton(context, item).takeIf { supportsLike }
        )
        session.setCustomLayout(commandButtons)
    }

    private fun updateCurrentFlow() {
        if (player.currentMediaItem == null && player.mediaItemCount > 0)
            throw Exception("This is possible")
        currentFlow.value = player.currentMediaItem?.let {
            val isPlaying = player.isPlaying && player.playbackState == Player.STATE_READY
            PlayerState.Current(player.currentMediaItemIndex, it, it.isLoaded, isPlaying)
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        updateCurrentFlow()
        updateCustomLayout()
        ResumptionUtils.saveIndex(context, player.currentMediaItemIndex)
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        updateCurrentFlow()
        updateCustomLayout()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        updateCurrentFlow()
        scope.launch { ResumptionUtils.saveQueue(context, player) }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateCustomLayout()
        ResumptionUtils.saveRepeat(context, repeatMode)
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        ResumptionUtils.saveShuffle(context, shuffleModeEnabled)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        updateCurrentFlow()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        updateCurrentFlow()
        ResumptionUtils.saveCurrentPos(context, player.currentPosition)
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int
    ) {
        ResumptionUtils.saveCurrentPos(context, player.currentPosition)
    }

    private val maxRetries = 3
    private val maxSingleItemRetries = 1
    private var currentRetries = 0
    private var last: KClass<*>? = null
    override fun onPlayerError(error: PlaybackException) {
        val cause = error.cause ?: error
        val mediaItem = player.currentMediaItem
        scope.launch { throwableFlow.emit(PlayerException(mediaItem, cause)) }

        val old = last
        last = cause.rootCause::class
        if (old != null && old == last) currentRetries++
        else currentRetries = 0

        if (mediaItem == null) return
        val index = player.currentMediaItemIndex
        val retries = mediaItem.retries

        if (currentRetries >= maxRetries) return
        if (retries >= maxSingleItemRetries) {
            val hasMore = index < player.mediaItemCount - 1
            if (!hasMore) return
            player.seekToNextMediaItem()
        } else {
            val newItem = MediaItemUtils.withRetry(mediaItem)
            player.replaceMediaItem(index, newItem)
        }
        player.prepare()
        player.play()
    }
}
