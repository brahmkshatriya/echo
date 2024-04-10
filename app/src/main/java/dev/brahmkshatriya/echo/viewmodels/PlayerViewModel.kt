package dev.brahmkshatriya.echo.viewmodels

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.session.MediaBrowser
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.player.PlayerListener
import dev.brahmkshatriya.echo.player.Queue
import dev.brahmkshatriya.echo.player.RadioListener.Companion.radio
import dev.brahmkshatriya.echo.ui.player.CheckBoxListener
import dev.brahmkshatriya.echo.utils.observe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val global: Queue,
    private val messageFlow: MutableSharedFlow<SnackBar.Message>,
    private val app: Application,
    val extensionListFlow: ExtensionModule.ExtensionListFlow,
    throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {


    val playPause: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val playPauseListener = CheckBoxListener {
        viewModelScope.launch { playPause.emit(it) }
    }

    val shuffle = MutableStateFlow(false)
    val shuffleListener = CheckBoxListener {
        viewModelScope.launch { shuffle.emit(it) }
    }


    val repeat = MutableStateFlow(0)
    var repeatEnabled = false
    fun onRepeat(it: Int) {
        if (repeatEnabled) viewModelScope.launch { repeat.emit(it) }
    }

    val seekTo: MutableSharedFlow<Long> = MutableSharedFlow()

    val seekToPrevious: MutableSharedFlow<Unit> = MutableSharedFlow()
    val seekToNext: MutableSharedFlow<Unit> = MutableSharedFlow()

    val clearQueueFlow = global.clearQueueFlow

    fun clearQueue() {
        viewModelScope.launch {
            global.clearQueue()
        }
    }

    val moveTrackFlow = global.moveTrackFlow
    fun moveQueueItems(new: Int, old: Int) {
        viewModelScope.launch {
            global.moveTrack(old, new)
        }
    }

    val removeTrackFlow = global.removeTrackFlow
    fun removeQueueItem(index: Int) {
        viewModelScope.launch {
            global.removeTrack(index)
        }
    }

    val addTrackFlow = global.addTrackFlow
    val audioIndexFlow = MutableSharedFlow<Int>()
    fun play(clientId: String, track: Track, playIndex: Int? = null) =
        play(clientId, listOf(track), playIndex)


    fun play(clientId: String, tracks: List<Track>, playIndex: Int? = null) {
        viewModelScope.launch {
            val pos = global.addTracks(clientId, tracks).first
            playIndex?.let { audioIndexFlow.emit(pos + it) }
        }
    }

    private fun playRadio(clientId: String, block: suspend RadioClient.() -> Playlist) {
        val client = extensionListFlow.getClient(clientId)
        viewModelScope.launch(Dispatchers.IO) {
            val position = radio(app, client, messageFlow, global) { tryWith { block(this) } }
            println("radio position : $position")
            position?.let { audioIndexFlow.emit(it) }
        }
    }

    fun radio(clientId: String, album: Album) = playRadio(clientId) { radio(album) }
    fun radio(clientId: String, artist: Artist) = playRadio(clientId) { radio(artist) }
    fun radio(clientId: String, playlist: Playlist) = playRadio(clientId) { radio(playlist) }


    companion object {
        fun LifecycleOwner.connectPlayerToUI(player: MediaBrowser, viewModel: PlayerViewModel) {
            val listener = PlayerListener(player, viewModel)
            player.addListener(listener)

            observe(viewModel.playPause) {
                if (it) player.play() else player.pause()
            }
            observe(viewModel.seekToPrevious) {
                player.seekToPrevious()
                player.playWhenReady = true
            }
            observe(viewModel.seekToNext) {
                player.seekToNext()
                player.playWhenReady = true
            }
            observe(viewModel.audioIndexFlow) {
                println("audioIndexFlow: $it : ${player.mediaItemCount}")
                if (it >= 0) player.seekToDefaultPosition(it)
            }
            observe(viewModel.seekTo) {
                player.seekTo(it)
            }
            observe(viewModel.repeat) {
                player.repeatMode = it
            }
            observe(viewModel.shuffle) {
                player.shuffleModeEnabled = it
            }
            observe(viewModel.addTrackFlow) { (index, item) ->
                player.addMediaItems(index, item)
                player.prepare()
                player.playWhenReady = true
            }
            observe(viewModel.moveTrackFlow) { (new, old) ->
                player.moveMediaItem(old, new)
            }
            observe(viewModel.removeTrackFlow) {
                player.removeMediaItem(it)
            }
            observe(viewModel.clearQueueFlow) {
                if (player.mediaItemCount == 0) return@observe
                player.pause()
                player.clearMediaItems()
                player.stop()
            }
        }
    }

    fun createException(exception: PlaybackException) {
        viewModelScope.launch {
            throwableFlow.emit(exception)
        }
    }


    fun updateList(mediaItems: List<String>, index: Int) {
        global.updateQueue(mediaItems)
        viewModelScope.launch {
            println("index : $index")
            global.currentIndexFlow.value = index
            _updatedFlow.emit(Unit)
        }
    }

    val current get() = global.current?.unloaded
    val currentIndex get() = global.currentIndexFlow.value

    val currentFlow = global.currentIndexFlow.map { current }
    private val _updatedFlow = MutableSharedFlow<Unit>()
    val listChangeFlow = merge(MutableStateFlow(Unit), _updatedFlow).map { global.queue }

    val progress = MutableStateFlow(0 to 0)
    val totalDuration = MutableStateFlow(0)

    val buffering = MutableStateFlow(false)
    val isPlaying = MutableStateFlow(false)
    val nextEnabled = MutableStateFlow(false)
    val previousEnabled = MutableStateFlow(false)
}