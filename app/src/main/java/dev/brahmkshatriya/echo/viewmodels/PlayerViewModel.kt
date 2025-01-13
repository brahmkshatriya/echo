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
import androidx.media3.session.MediaController
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.ExceptionActivity
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.PlayerCommands.radioCommand
import dev.brahmkshatriya.echo.playback.PlayerCommands.sleepTimer
import dev.brahmkshatriya.echo.playback.PlayerException
import dev.brahmkshatriya.echo.playback.ResumptionUtils
import dev.brahmkshatriya.echo.playback.listeners.Radio
import dev.brahmkshatriya.echo.ui.editplaylist.EditPlaylistViewModel.Companion.deletePlaylist
import dev.brahmkshatriya.echo.ui.player.PlayerUiListener
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.KEEP_QUEUE
import dev.brahmkshatriya.echo.utils.getSerialized
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
    val currentServers: MutableStateFlow<Map<String, Streamable.Media.Server>>,
    val cache: SimpleCache,
    val audioSessionFlow: MutableStateFlow<Int>,
    private val mutableMessageFlow: MutableSharedFlow<SnackBar.Message>,
    throwableFlow: MutableSharedFlow<Throwable>,
) : CatchingViewModel(throwableFlow) {

    val browser = MutableStateFlow<MediaController?>(null)
    fun withBrowser(block: (MediaController) -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            val browser = browser.first { it != null }!!
            runCatching { block(browser) }.getOrElse { throwableFlow.emit(it) }
        }
    }

    fun likeTrack(isLiked: Boolean) = withBrowser {
        val future = it.setRating(ThumbRating(isLiked))
        app.listenFuture(future) { sessionResult ->
            val result = sessionResult.getOrThrow()
            if (result.resultCode != RESULT_SUCCESS) {
                val exception =
                    result.extras.getSerialized<ExceptionActivity.ExceptionDetails>("error")
                        ?: ExceptionActivity.ExceptionDetails("IO Error", "")
                createException(exception)
            }
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
            when (lists) {
                is EchoMediaItem.Lists.AlbumItem -> {
                    extension.get<AlbumClient, List<Track>>(throwableFlow) {
                        loadTracks(lists.album).loadAll()
                    }
                }

                is EchoMediaItem.Lists.PlaylistItem -> {
                    extension.get<PlaylistClient, List<Track>>(throwableFlow) {
                        loadTracks(lists.playlist).loadAll()
                    }
                }

                is EchoMediaItem.Lists.RadioItem -> {
                    extension.get<RadioClient, List<Track>>(throwableFlow) {
                        loadTracks(lists.radio).loadAll()
                    }
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
        val index = if (end) it.mediaItemCount else {
            val curr = currentFlow.value?.index ?: 0
            curr + 1
        }
        val shouldPlay = it.mediaItemCount == 0
        it.addMediaItems(index, mediaItems)
        it.prepare()
        if (shouldPlay) it.playWhenReady = true
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
            addToQueue(clientId, radio.toMediaItem(), trackList, true)
            withBrowser { play(it.mediaItemCount - 1) }
        }
    }

    companion object {
        @SuppressLint("UnsafeOptInUsageError")
        fun connectPlayerToUI(
            player: MediaController,
            viewModel: PlayerViewModel
        ) {
            viewModel.browser.value = player
            player.addListener(PlayerUiListener(player, viewModel))

            viewModel.run {
                val keepQueue = settings.getBoolean(KEEP_QUEUE, true)
                if (keepQueue && !player.isPlaying) viewModelScope.launch {
                    extensionListFlow.first { it != null }
                    ResumptionUtils.recoverPlaylist(app).apply {
                        player.setMediaItems(mediaItems, startIndex, startPositionMs)
                        player.prepare()
                    }
                }
            }
        }
    }

    private fun createException(exceptionDetails: ExceptionActivity.ExceptionDetails) =
        withBrowser {
            viewModelScope.launch {
                throwableFlow.emit(
                    PlayerException(exceptionDetails, it.currentMediaItem)
                )
            }
        }


    var list: List<MediaItem> = listOf()

    val listUpdateFlow = MutableSharedFlow<Unit>()

    val progress = MutableStateFlow(0 to 0)
    val discontinuity = MutableStateFlow(0L)
    val totalDuration = MutableStateFlow<Int?>(null)

    val buffering = MutableStateFlow(false)
    val isPlaying = MutableStateFlow(false)
    val nextEnabled = MutableStateFlow(false)
    val previousEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(0)
    val shuffleMode = MutableStateFlow(false)

    val shareLink = MutableSharedFlow<String>()
    fun onShare(client: ShareClient, item: EchoMediaItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val link = client.onShare(item)
            shareLink.emit(link)
        }
    }

    fun deletePlaylist(clientId: String, playlist: Playlist) =
        deletePlaylist(extensionListFlow, mutableMessageFlow, app, clientId, playlist)

    fun setSleepTimer(ms: Long) {
        withBrowser { it.sendCustomCommand(sleepTimer, bundleOf("ms" to ms)) }
    }
}