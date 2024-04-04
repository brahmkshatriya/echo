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
    private val extensionListFlow: ExtensionModule.ExtensionListFlow,
    private val messageFlow: MutableSharedFlow<SnackBarViewModel.Message>,
    private val app: Application,
    throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {

    val list = global.queue
    val currentIndex = global.currentIndex
    val listChangeFlow = merge(
        MutableStateFlow(null),
        global.addTrackFlow,
        global.removeTrackFlow,
        global.moveTrackFlow,
        global.clearQueueFlow
    ).map { global.queue }

    val audioIndexFlow = MutableSharedFlow<Int>()
    val playPause: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val seekTo: MutableSharedFlow<Long> = MutableSharedFlow()
    val seekToPrevious: MutableSharedFlow<Unit> = MutableSharedFlow()
    val seekToNext: MutableSharedFlow<Unit> = MutableSharedFlow()

    val clearQueueFlow = global.clearQueueFlow
    val addTrackFlow = global.addTrackFlow
    val moveTrackFlow = global.moveTrackFlow
    val removeTrackFlow = global.removeTrackFlow

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

    fun play(clientId: String, track: Track, playIndex: Int? = null) =
        play(clientId, listOf(track), playIndex)


    fun play(clientId: String, tracks: List<Track>, playIndex: Int? = null) {
        viewModelScope.launch {
            val pos = global.addTracks(clientId, tracks).first
            playIndex?.let { audioIndexFlow.emit(pos + it) }
        }
    }

    fun addToQueue(clientId: String, track: Track) {
        viewModelScope.launch { global.addTrack(clientId, track) }
    }

    private fun playRadio(clientId: String, block: suspend RadioClient.() -> Playlist) {
        val client = extensionListFlow.getClient(clientId)
        viewModelScope.launch(Dispatchers.IO) {
            radio(app, client, messageFlow, global) { tryWith { block(this) } }
            audioIndexFlow.emit(0)
        }
    }

    fun radio(clientId: String, album: Album) = playRadio(clientId) { radio(album) }
    fun radio(clientId: String, artist: Artist) = playRadio(clientId) { radio(artist) }
    fun radio(clientId: String, playlist: Playlist) = playRadio(clientId) { radio(playlist) }



    fun getTrack(mediaId: String?) = global.getTrack(mediaId)?.track

    fun createException(exception: PlaybackException) {
        viewModelScope.launch {
            throwableFlow.emit(exception)
        }
    }

    fun changeCurrent(index: Int?) {
        global.currentIndex.value = index
    }

    val playPauseListener = CheckBoxListener {
        viewModelScope.launch { playPause.emit(it) }
    }

    val track = MutableStateFlow(global.queue.firstOrNull()?.track)

    val progress = MutableStateFlow(0 to 0)
    val totalDuration = MutableStateFlow(0)

    val buffering = MutableStateFlow(false)
    val isPlaying = MutableStateFlow(false)
    val nextEnabled = MutableStateFlow(false)
    val previousEnabled = MutableStateFlow(false)

    val repeat = MutableStateFlow(0)
    val shuffle = MutableStateFlow(false)

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
}