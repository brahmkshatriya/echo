package dev.brahmkshatriya.echo.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.ThumbRating
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.session.LibraryResult.RESULT_SUCCESS
import androidx.media3.session.MediaBrowser
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.PlayerCommands.radioCommand
import dev.brahmkshatriya.echo.playback.Radio
import dev.brahmkshatriya.echo.playback.ResumptionUtils
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.ui.player.CheckBoxListener
import dev.brahmkshatriya.echo.ui.player.PlayerUiListener
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.KEEP_QUEUE
import dev.brahmkshatriya.echo.utils.getSerial
import dev.brahmkshatriya.echo.utils.listenFuture
import dev.brahmkshatriya.echo.utils.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
@OptIn(UnstableApi::class)
class PlayerViewModel @Inject constructor(
    val settings: SharedPreferences,
    val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    val app: Application,
    val currentFlow: MutableStateFlow<Current?>,
    val radioStateFlow: MutableStateFlow<Radio.State>,
    val cache: SimpleCache,
    throwableFlow: MutableSharedFlow<Throwable>,
) : CatchingViewModel(throwableFlow) {

    var browser: MediaBrowser? = null
    private fun withBrowser(block: (MediaBrowser) -> Unit) {
        val browser = browser
        if (browser != null) viewModelScope.launch(Dispatchers.Main) {
            runCatching { block(browser) }.getOrElse {
                createException(it)
            }
        }
//        else viewModelScope.launch {
//            throwableFlow.emit(IllegalStateException("Browser not connected"))
//        }
    }

    val playPauseListener = CheckBoxListener {
        withBrowser { browser -> if (it) browser.play() else browser.pause() }
    }

    val shuffleListener = CheckBoxListener {
        withBrowser { browser -> browser.shuffleModeEnabled = it }
    }

    var repeatEnabled = false
    fun onRepeat(it: Int) {
        if (repeatEnabled) withBrowser { browser ->
            browser.repeatMode = it
        }
    }

    fun seekTo(position: Long) = withBrowser { it.seekTo(position) }
    fun seekToPrevious() = withBrowser { it.seekToPrevious() }
    fun seekToNext() = withBrowser { it.seekToNext() }

    val likeListener = CheckBoxListener { likeTrack(it) }
    private fun likeTrack(isLiked: Boolean) = withBrowser {
        val old = this.isLiked.value
        this.isLiked.value = isLiked
        val future = it.setRating(ThumbRating(isLiked))
        app.listenFuture(future) { sessionResult ->
            val result = sessionResult.getOrThrow()
            if (result.resultCode != RESULT_SUCCESS) {
                val exception = result.extras.getSerial<Throwable>("error")
                    ?: Exception("Error : ${result.resultCode}")
                createException(exception)
                this.isLiked.value = old
            }
            this.isLiked.value = result.extras.getBoolean("liked")
        }
    }

    fun play(position: Int) = withBrowser { it.seekToDefaultPosition(position) }

    fun clearQueue() = withBrowser { it.clearMediaItems() }
    fun moveQueueItems(new: Int, old: Int) = withBrowser { it.moveMediaItem(old, new) }
    fun removeQueueItem(index: Int) = withBrowser { it.removeMediaItem(index) }

    fun play(clientId: String, track: Track, playIndex: Int? = null) =
        play(clientId, null, listOf(track), playIndex)

    private suspend fun getTracks(clientId: String, lists: EchoMediaItem.Lists) =
        withContext(Dispatchers.IO) {
            val extension = extensionListFlow.getExtension(clientId) ?: return@withContext null
            val client = extension.client
            when (lists) {
                is EchoMediaItem.Lists.AlbumItem -> {
                    if (client is AlbumClient) tryWith(extension.info) {
                        client.loadTracks(lists.album).loadAll()
                    }
                    else null
                }

                is EchoMediaItem.Lists.PlaylistItem -> {
                    if (client is PlaylistClient) tryWith(extension.info) {
                        client.loadTracks(lists.playlist).loadAll()
                    }
                    else null
                }
            }
        }

    fun play(
        clientId: String,
        lists: EchoMediaItem.Lists,
        playIndex: Int?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val tracks = getTracks(clientId, lists) ?: return@launch
            play(clientId, lists, tracks, playIndex)
        }
    }

    fun play(
        clientId: String,
        context: EchoMediaItem?,
        tracks: List<Track>,
        playIndex: Int? = null
    ) {
        withBrowser {
            val mediaItems = tracks.map { track ->
                MediaItemUtils.build(settings, track, clientId, context)
            }
            it.setMediaItems(mediaItems, playIndex ?: 0, 0)
            it.prepare()
            it.playWhenReady = true
        }
    }

    fun addToQueue(clientId: String, track: Track, end: Boolean) =
        addToQueue(clientId, null, listOf(track), end)

    fun addToQueue(
        clientId: String,
        lists: EchoMediaItem.Lists,
        end: Boolean
    ) = viewModelScope.launch(Dispatchers.IO) {
        val tracks = getTracks(clientId, lists) ?: return@launch
        addToQueue(clientId, lists, tracks, end)
    }

    private fun addToQueue(
        clientId: String,
        context: EchoMediaItem?,
        tracks: List<Track>,
        end: Boolean
    ) = withBrowser {
        val mediaItems = tracks.map { track ->
            MediaItemUtils.build(settings, track, clientId, context)
        }
        val index = if (end) it.mediaItemCount else 1
        it.addMediaItems(index, mediaItems)
        it.prepare()
    }

    fun radio(clientId: String, item: EchoMediaItem) {
        withBrowser {
            it.sendCustomCommand(
                radioCommand,
                bundleOf("clientId" to clientId, "item" to item.toJson())
            )
        }
    }

    fun radioPlay(subIndex: Int) {
        val state = radioStateFlow.value
        if (state !is Radio.State.Loaded) return
        state.run {
            val index = played + subIndex + 1
            val trackList = tracks.take(index + 1).drop(played + 1).ifEmpty { null } ?: return
            radioStateFlow.value = state.copy(played = index)
            addToQueue(clientId, playlist.toMediaItem(), trackList, true)
            withBrowser { play(it.mediaItemCount - 1) }
        }
    }

    companion object {
        @SuppressLint("UnsafeOptInUsageError")
        fun connectBrowserToUI(
            browser: MediaBrowser,
            viewModel: PlayerViewModel
        ) {
            viewModel.browser = browser
            browser.addListener(PlayerUiListener(browser, viewModel))

            viewModel.run {
                val keepQueue = settings.getBoolean(KEEP_QUEUE, true)
                if (keepQueue && !browser.isPlaying) viewModelScope.launch {
                    extensionListFlow.first { it != null }
                    ResumptionUtils.recoverPlaylist(app).apply {
                        browser.setMediaItems(mediaItems, startIndex, startPositionMs)
                        browser.prepare()
                    }
                }
            }
        }
    }

    fun createException(exception: Throwable) {
        withBrowser {
            viewModelScope.launch {
                throwableFlow.emit(
                    PlayerException(exception.cause ?: exception, it.currentMediaItem)
                )
            }
        }
    }

    data class PlayerException(
        override val cause: Throwable,
        val mediaItem: MediaItem?
    ) : Throwable(cause.message)

    var list: List<MediaItem> = listOf()

    val listUpdateFlow = MutableSharedFlow<Unit>()

    val isLiked = MutableStateFlow(false)
    val progress = MutableStateFlow(0 to 0)
    val discontinuity = MutableStateFlow(0L)
    val totalDuration = MutableStateFlow<Int?>(null)

    val buffering = MutableStateFlow(false)
    val isPlaying = MutableStateFlow(false)
    val nextEnabled = MutableStateFlow(false)
    val previousEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(0)
    val shuffleMode = MutableStateFlow(false)
}