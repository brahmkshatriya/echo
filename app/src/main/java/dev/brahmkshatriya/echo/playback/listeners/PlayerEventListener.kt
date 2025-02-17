package dev.brahmkshatriya.echo.playback.listeners

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaSession
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.extensions.isClient
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.retries
import dev.brahmkshatriya.echo.playback.PlayerCommands.getLikeButton
import dev.brahmkshatriya.echo.playback.PlayerCommands.getRepeatButton
import dev.brahmkshatriya.echo.playback.PlayerException
import dev.brahmkshatriya.echo.playback.ResumptionUtils
import dev.brahmkshatriya.echo.playback.StreamableLoadingException
import dev.brahmkshatriya.echo.ui.exception.AppException
import dev.brahmkshatriya.echo.ui.exception.ExceptionFragment.Companion.toExceptionDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerEventListener(
    private val context: Context,
    private val scope: CoroutineScope,
    private val session: MediaSession,
    private val currentFlow: MutableStateFlow<Current?>,
    private val extensionList: MutableStateFlow<List<MusicExtension>?>,
    private val throwableFlow: MutableSharedFlow<Throwable>
) : Player.Listener {

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable { updateCurrent() }

    private fun updateCurrent() {
        handler.removeCallbacks(runnable)
        ResumptionUtils.saveCurrentPos(context, player.currentPosition)
        handler.postDelayed(runnable, 1000)
    }

    val player get() = session.player

    private fun updateCustomLayout() = scope.launch(Dispatchers.Main) {
        val item = player.currentMediaItem ?: return@launch
        val supportsLike = withContext(Dispatchers.IO) {
            extensionList.getExtension(item.extensionId)?.isClient<TrackLikeClient>()
                ?: false
        }
        val commandButtons = listOfNotNull(
            getRepeatButton(context, player.repeatMode),
            getLikeButton(context, item).takeIf { supportsLike }
        )
        session.setCustomLayout(commandButtons)
    }

    private fun updateCurrentFlow() {
        currentFlow.value = player.currentMediaItem?.let {
            val isPlaying = player.isPlaying && player.playbackState == Player.STATE_READY
            Current(player.currentMediaItemIndex, it, it.isLoaded, isPlaying)
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        updateCurrentFlow()
        updateCustomLayout()
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
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        updateCurrentFlow()
        updateCustomLayout()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        updateCurrentFlow()
        updateCurrent()
    }

    private val maxRetries = 1
    private var currentRetries = 0
    private var last: Pair<String, Throwable>? = null
    override fun onPlayerError(error: PlaybackException) {
        val cause = error.cause?.cause ?: error.cause ?: error

        val exception = if (cause is StreamableLoadingException) cause.cause
        else PlayerException(cause.toExceptionDetails(context), player.currentMediaItem)
        scope.launch { throwableFlow.emit(exception) }

        if (cause !is StreamableLoadingException) return
        if (cause.cause !is AppException.Other) return

        val mediaItem = cause.mediaItem
        val index = player.currentMediaItemIndex.takeIf {
            player.currentMediaItem?.mediaId == mediaItem.mediaId
        } ?: return

        val old = last
        val new = cause.cause.cause
        last = mediaItem.mediaId to new
        if (old != null && old.first != mediaItem.mediaId && old.second::class == new::class)
            currentRetries++
        else currentRetries = 0

        if (currentRetries >= maxRetries) return
        if (!player.playWhenReady) return

        val retries = mediaItem.retries
        if (retries >= maxRetries) {
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
