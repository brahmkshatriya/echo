package dev.brahmkshatriya.echo.viewmodels

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewModelScope
import androidx.media3.common.ThumbRating
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionResult.RESULT_ERROR_UNKNOWN
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.PlayerListener
import dev.brahmkshatriya.echo.playback.Queue
import dev.brahmkshatriya.echo.playback.recoverQueue
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.ui.player.CheckBoxListener
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.KEEP_QUEUE
import dev.brahmkshatriya.echo.utils.getSerial
import dev.brahmkshatriya.echo.utils.listenFuture
import dev.brahmkshatriya.echo.utils.observe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val global: Queue,
    val settings: SharedPreferences,
    val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    throwableFlow: MutableSharedFlow<Throwable>,
    private val listener: PlayerListener,
) : CatchingViewModel(throwableFlow) {

    override fun onInitialize() {
        listener.setViewModel(this)
    }

    val playPause = MutableSharedFlow<Boolean>()
    val playPauseListener = CheckBoxListener {
        viewModelScope.launch { playPause.emit(it) }
    }

    val shuffle = global.shuffle
    val shuffleListener = CheckBoxListener {
        viewModelScope.launch { shuffle.emit(it) }
    }

    val repeat = global.repeat
    var repeatEnabled = false
    fun onRepeat(it: Int) {
        if (repeatEnabled) viewModelScope.launch { repeat.emit(it) }
    }

    val seekTo = MutableSharedFlow<Long>()
    val seekToPrevious = MutableSharedFlow<Unit>()
    val seekToNext = MutableSharedFlow<Unit>()

    val likeTrack = MutableSharedFlow<Pair<String, Boolean>>()

    fun clearQueue() {
        viewModelScope.launch {
            global.clearQueue()
        }
    }

    fun moveQueueItems(new: Int, old: Int) {
        viewModelScope.launch {
            global.moveTrack(old, new)
        }
    }

    fun removeQueueItem(index: Int) {
        viewModelScope.launch {
            global.removeTrack(index)
        }
    }

    val audioIndexFlow = MutableSharedFlow<Int>()
    fun play(clientId: String, track: Track, playIndex: Int? = null) =
        play(clientId, null, listOf(track), playIndex)

    private suspend fun getTracks(clientId: String, lists: EchoMediaItem.Lists) =
        withContext(Dispatchers.IO) {
            val client = extensionListFlow.getExtension(clientId)?.client
                ?: return@withContext null
            when (lists) {
                is EchoMediaItem.Lists.AlbumItem -> {
                    if (client is AlbumClient) tryWith { client.loadTracks(lists.album).loadFirst() }
                    else null
                }

                is EchoMediaItem.Lists.PlaylistItem -> {
                    if (client is PlaylistClient) tryWith {
                        client.loadTracks(lists.playlist).loadFirst()
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
        viewModelScope.launch {
            global.clearQueue()
            val pos = global.addTracks(clientId, context, tracks).first
            playIndex?.let { audioIndexFlow.emit(pos + it) }
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
    ) {
        viewModelScope.launch {
            val index = if (end) global.queue.size else 1
            global.addTracks(clientId, context, tracks, index)
        }
    }

    private fun playRadio(clientId: String, block: suspend RadioClient.() -> Playlist) {
        val extension = extensionListFlow.getExtension(clientId)
        viewModelScope.launch(Dispatchers.IO) {
            val position = listener.radio(extension) { block(this) }
            position?.let { audioIndexFlow.emit(it) }
        }
    }

    val onLiked = global.onLiked
    fun likeTrack(track: Queue.StreamableTrack, isLiked: Boolean) = viewModelScope.launch {
        likeTrack.emit(track.unloaded.id to isLiked)
    }


    fun radio(clientId: String, track: Track) = playRadio(clientId) { radio(track) }
    fun radio(clientId: String, album: Album) = playRadio(clientId) { radio(album) }
    fun radio(clientId: String, artist: Artist) = playRadio(clientId) { radio(artist) }
    fun radio(clientId: String, playlist: Playlist) = playRadio(clientId) { radio(playlist) }


    companion object {
        fun AppCompatActivity.connectBrowserToUI(
            browser: MediaBrowser,
            viewModel: PlayerViewModel
        ) {
            viewModel.initialize()
            observe(viewModel.playPause) {
                if (it) browser.play() else browser.pause()
            }
            observe(viewModel.seekToPrevious) {
                browser.seekToPrevious()
                browser.playWhenReady = true
            }
            observe(viewModel.seekToNext) {
                browser.seekToNext()
                browser.playWhenReady = true
            }
            observe(viewModel.audioIndexFlow) {
                if (it >= 0) {
                    browser.seekToDefaultPosition(it)
                    browser.playWhenReady = true
                }
            }
            observe(viewModel.seekTo) {
                browser.seekTo(it)
            }

            observe(viewModel.likeTrack) { (mediaId, isLiked) ->
                val track = viewModel.global.getTrack(mediaId) ?: return@observe
                if (track.liked == isLiked) return@observe

                val future = browser.setRating(mediaId, ThumbRating(isLiked))
                listenFuture(future) {
                    val result = it.getOrThrow()
                    if (result.resultCode == RESULT_ERROR_UNKNOWN) {
                        val exception = result.extras.getSerial<Throwable>("error")!!
                        viewModel.createException(exception)
                    }
                }
            }

            val keepQueue = viewModel.settings.getBoolean(KEEP_QUEUE, true)
            if (keepQueue && browser.mediaItemCount == 0)
                browser.sendCustomCommand(recoverQueue, Bundle.EMPTY)
        }
    }

    fun createException(exception: Throwable) {
        viewModelScope.launch {
            val streamableTrack = global.current
            val currentAudio = global.currentAudioFlow.value
            throwableFlow.emit(
                PlayerException(exception.cause ?: exception, streamableTrack, currentAudio)
            )
        }
    }

    data class PlayerException(
        override val cause: Throwable,
        val streamableTrack: Queue.StreamableTrack?,
        val currentAudio: StreamableAudio?
    ) : Throwable(cause.message)


    val current get() = global.current
    val currentIndex get() = global.currentIndex

    val currentFlow =
        merge(MutableStateFlow(Unit), global.currentIndexFlow).map { global.currentIndex }

    val listChangeFlow = merge(MutableStateFlow(Unit), global.updateFlow).map { global.queue }

    val progress = MutableStateFlow(0 to 0)
    val totalDuration = MutableStateFlow<Int?>(null)

    val buffering = MutableStateFlow(false)
    val isPlaying = MutableStateFlow(false)
    val nextEnabled = MutableStateFlow(false)
    val previousEnabled = MutableStateFlow(false)
}