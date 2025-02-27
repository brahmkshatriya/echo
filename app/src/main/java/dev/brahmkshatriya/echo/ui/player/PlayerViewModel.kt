package dev.brahmkshatriya.echo.ui.player

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.getController
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.ResumptionUtils.recoverPlaylist
import dev.brahmkshatriya.echo.playback.ResumptionUtils.recoverRepeat
import dev.brahmkshatriya.echo.playback.ResumptionUtils.recoverShuffle
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlayerViewModel(
    val app: App,
    val playerState: PlayerState,
    val settings: SharedPreferences
) : ViewModel() {

    val browser = MutableStateFlow<MediaController?>(null)
    private fun withBrowser(block: (MediaController) -> Unit) {
        viewModelScope.launch {
            val browser = browser.first { it != null }!!
            block(browser)
        }
    }

    var queue: List<MediaItem> = emptyList()
    val queueFlow = MutableSharedFlow<Unit>()
    private val context = app.context
    val controllerFutureRelease = getController(context) { player ->
        browser.value = player
        player.addListener(PlayerUiListener(player, this))

        if (player.mediaItemCount != 0) return@getController
        if (!settings.getBoolean(KEEP_QUEUE, true)) return@getController

        player.shuffleModeEnabled = context.recoverShuffle() == true
        player.repeatMode = context.recoverRepeat() ?: 0
        val (items, index, pos) = context.recoverPlaylist(true)
        player.setMediaItems(items, index, pos)
        player.prepare()
    }

    override fun onCleared() {
        super.onCleared()
        controllerFutureRelease()
    }

    fun play(position: Int) {
        withBrowser {
            it.seekTo(position, 0)
            it.playWhenReady = true
        }
    }

    fun seek(position: Int) {
        withBrowser { it.seekTo(position, 0) }
    }

    fun removeQueueItem(position: Int) {
        withBrowser { it.removeMediaItem(position) }
    }

    fun moveQueueItems(fromPos: Int, toPos: Int) {
        withBrowser { it.moveMediaItem(fromPos, toPos) }
    }

    fun clearQueue() {
        withBrowser { it.clearMediaItems() }
    }

    val progress = MutableStateFlow(0 to 0)
    val discontinuity = MutableStateFlow(0L)
    val totalDuration = MutableStateFlow<Int?>(null)

    val buffering = MutableStateFlow(false)
    val isPlaying = MutableStateFlow(false)
    val nextEnabled = MutableStateFlow(false)
    val previousEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(0)
    val shuffleMode = MutableStateFlow(false)

    companion object {
        const val KEEP_QUEUE = "keep_queue"
    }
}