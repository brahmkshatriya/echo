package dev.brahmkshatriya.echo.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaLibraryService
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.PlayerCommands.getLikeButton
import dev.brahmkshatriya.echo.playback.PlayerCommands.getRepeatButton
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.extensions.isClient
import kotlinx.coroutines.flow.MutableStateFlow

class PlayerEventListener(
    private val context: Context,
    private val session: MediaLibraryService.MediaLibrarySession,
    private val currentFlow: MutableStateFlow<Current?>,
    private val extensionList: MutableStateFlow<List<MusicExtension>?>,
) : Player.Listener {

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable { updateCurrent() }

    private fun updateCurrent() {
        handler.removeCallbacks(runnable)
        if (player.isPlaying) ResumptionUtils.saveCurrentPos(context, player.currentPosition)
        handler.postDelayed(runnable, 1000)
    }

    val player get() = session.player

    private fun updateCustomLayout() {
        val item = player.currentMediaItem ?: return
        val supportsLike = extensionList.getExtension(item.clientId)?.isClient<LibraryClient>()
            ?: false

        val commandButtons = listOfNotNull(
            getRepeatButton(context, player.repeatMode),
            getLikeButton(context, item).takeIf { supportsLike }
        )
        session.setCustomLayout(commandButtons)
    }

    private fun updateCurrentFlow() {
        currentFlow.value = player.currentMediaItem?.let {
            Current(player.currentMediaItemIndex, it, it.isLoaded)
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        updateCurrentFlow()
        updateCustomLayout()
        updateCurrent()
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        updateCurrentFlow()
        updateCustomLayout()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        updateCurrentFlow()
        ResumptionUtils.saveQueue(context, player.currentMediaItemIndex, player.mediaItems)
    }

    private val Player.mediaItems get() = (0 until mediaItemCount).map { getMediaItemAt(it) }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateCustomLayout()
    }

}
