package dev.brahmkshatriya.echo.ui.player

import android.content.SharedPreferences
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.bundle.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.ThumbRating
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.session.MediaController
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.MediaItemUtils.serverWithDownloads
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.PlayerCommands.addToNextCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.addToQueueCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.playCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.radioCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.resumeCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.sleepTimer
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.getController
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.utils.ContextUtils.listenFuture
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@OptIn(UnstableApi::class)
class PlayerViewModel(
    val app: App,
    val playerState: PlayerState,
    val settings: SharedPreferences,
    val cache: SimpleCache,
    extensionLoader: ExtensionLoader,
    downloader: Downloader,
) : ViewModel() {
    private val extensions = extensionLoader
    private val downloadFlow = downloader.flow

    val browser = MutableStateFlow<MediaController?>(null)
    private fun withBrowser(block: suspend (MediaController) -> Unit) {
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

        player.sendCustomCommand(resumeCommand, Bundle.EMPTY)
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

    fun seekTo(pos: Long) {
        withBrowser { it.seekTo(pos) }
    }

    fun seekToAdd(position: Int) {
        withBrowser { it.seekTo(max(0, it.currentPosition + position)) }
    }

    fun setPlaying(isPlaying: Boolean) {
        withBrowser {
            it.prepare()
            it.playWhenReady = isPlaying
        }
    }

    fun next() {
        withBrowser { it.seekToNextMediaItem() }
    }

    fun previous() {
        withBrowser { it.seekToPreviousMediaItem() }
    }

    fun setShuffle(isShuffled: Boolean, changeCurrent: Boolean = false) {
        withBrowser {
            it.shuffleModeEnabled = isShuffled
            if (changeCurrent) it.seekTo(0, 0)
        }
    }

    fun setRepeat(repeatMode: Int) {
        withBrowser { it.repeatMode = repeatMode }
    }

    suspend fun isTrackClient(extensionId: String): Boolean = withContext(Dispatchers.IO) {
        extensions.music.getExtension(extensionId)?.isClient<TrackLikeClient>() ?: false
    }

    private fun createException(throwable: Throwable) {
        viewModelScope.launch { app.throwFlow.emit(throwable) }
    }

    fun likeCurrent(isLiked: Boolean) = withBrowser { controller ->
        val future = controller.setRating(ThumbRating(isLiked))
        app.context.listenFuture(future) { sessionResult ->
            sessionResult.getOrElse { createException(it) }
        }
    }

    fun setSleepTimer(timer: Long) {
        withBrowser { it.sendCustomCommand(sleepTimer, bundleOf("ms" to timer)) }
    }

    fun changeTrackSelection(trackGroup: TrackGroup, index: Int) {
        withBrowser {
            it.trackSelectionParameters = it.trackSelectionParameters
                .buildUpon()
                .clearOverride(trackGroup)
                .addOverride(TrackSelectionOverride(trackGroup, index))
                .build()
        }
    }

    private fun changeCurrent(newItem: MediaItem) {
        withBrowser { player ->
            val oldPosition = player.currentPosition
            player.replaceMediaItem(player.currentMediaItemIndex, newItem)
            player.prepare()
            player.seekTo(oldPosition)
        }
    }

    fun changeServer(server: Streamable) {
        val item = playerState.current.value?.mediaItem ?: return
        val index = item.serverWithDownloads(app.context).indexOf(server).takeIf { it != -1 }
            ?: return
        changeCurrent(MediaItemUtils.buildServer(item, index))
    }

    fun changeBackground(background: Streamable?) {
        val item = playerState.current.value?.mediaItem ?: return
        val index = item.track.backgrounds.indexOf(background)
        changeCurrent(MediaItemUtils.buildBackground(item, index))
    }

    fun changeSubtitle(subtitle: Streamable?) {
        val item = playerState.current.value?.mediaItem ?: return
        val index = item.track.subtitles.indexOf(subtitle)
        changeCurrent(MediaItemUtils.buildSubtitle(item, index))
    }

    fun changeCurrentSource(index: Int) {
        val item = playerState.current.value?.mediaItem ?: return
        changeCurrent(MediaItemUtils.buildSource(item, index))
    }

    fun setQueue(id: String, list: List<Track>, index: Int, context: EchoMediaItem?) {
        withBrowser { controller ->
            val mediaItems = list.map {
                MediaItemUtils.build(app.context, downloadFlow.value, it, id, context)
            }
            controller.setMediaItems(mediaItems, index, 0)
            controller.prepare()
        }
    }

    fun radio(id: String, item: EchoMediaItem) = viewModelScope.launch {
        app.messageFlow.emit(
            Message(app.context.getString(R.string.loading_radio_for_x, item.title))
        )
        withBrowser {
            it.sendCustomCommand(radioCommand, Bundle().apply {
                putString("extId", id)
                putSerialized("item", item)
            })
        }
    }

    fun play(id: String, item: EchoMediaItem, loaded: Boolean) = viewModelScope.launch {
        if (item !is EchoMediaItem.TrackItem) app.messageFlow.emit(
            Message(app.context.getString(R.string.playing_x, item.title))
        )
        withBrowser {
            it.sendCustomCommand(playCommand, Bundle().apply {
                putString("extId", id)
                putSerialized("item", item)
                putBoolean("loaded", loaded)
                putBoolean("shuffle", false)
            })
        }
    }

    fun shuffle(id: String, item: EchoMediaItem, loaded: Boolean) = viewModelScope.launch {
        if (item !is EchoMediaItem.TrackItem) app.messageFlow.emit(
            Message(app.context.getString(R.string.shuffling_x, item.title))
        )
        withBrowser {
            it.sendCustomCommand(playCommand, Bundle().apply {
                putString("extId", id)
                putSerialized("item", item)
                putBoolean("loaded", loaded)
                putBoolean("shuffle", true)
            })
        }
    }


    fun addToQueue(id: String, item: EchoMediaItem, loaded: Boolean) = viewModelScope.launch {
        if (item !is EchoMediaItem.TrackItem) app.messageFlow.emit(
            Message(app.context.getString(R.string.adding_x_to_queue, item.title))
        )
        withBrowser {
            it.sendCustomCommand(addToQueueCommand, Bundle().apply {
                putString("extId", id)
                putSerialized("item", item)
                putBoolean("loaded", loaded)
            })
        }
    }

    fun addToNext(id: String, item: EchoMediaItem, loaded: Boolean) = viewModelScope.launch {
        if (item !is EchoMediaItem.TrackItem) app.messageFlow.emit(
            Message(app.context.getString(R.string.adding_x_to_next, item.title))
        )
        withBrowser {
            it.sendCustomCommand(addToNextCommand, Bundle().apply {
                putString("extId", id)
                putSerialized("item", item)
                putBoolean("loaded", loaded)
            })
        }
    }

    val progress = MutableStateFlow(0L to 0L)
    val discontinuity = MutableStateFlow(0L)
    val totalDuration = MutableStateFlow<Long?>(null)

    val buffering = MutableStateFlow(false)
    val isPlaying = MutableStateFlow(false)
    val nextEnabled = MutableStateFlow(false)
    val previousEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(0)
    val shuffleMode = MutableStateFlow(false)

    val tracks = MutableStateFlow<Tracks?>(null)

    companion object {
        const val KEEP_QUEUE = "keep_queue"
    }
}