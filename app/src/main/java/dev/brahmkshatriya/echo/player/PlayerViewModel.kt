package dev.brahmkshatriya.echo.player

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.ExtensionFlow
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.tryWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val global: Queue,
    trackFlow: ExtensionFlow,
    private val exceptionFlow: MutableSharedFlow<Exception>,
    application: Application
) : ViewModel() {

    val fromNotification: MutableSharedFlow<Boolean> = MutableSharedFlow()

    private var trackClient: TrackClient? = null

    init {
        viewModelScope.observe(trackFlow.flow) {
            trackClient = it as? TrackClient
        }
    }

    val audioIndexFlow = MutableSharedFlow<Int>()
    val playPause: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val seekTo: MutableSharedFlow<Long> = MutableSharedFlow()
    val seekToPrevious: MutableSharedFlow<Unit> = MutableSharedFlow()
    val seekToNext: MutableSharedFlow<Unit> = MutableSharedFlow()
    val repeat: MutableSharedFlow<Int> = MutableSharedFlow()
    val shuffle: MutableSharedFlow<Boolean> = MutableSharedFlow()

    val clearQueueFlow = global.clearQueueFlow
    val addTrackFlow = global.addTrackFlow
    val moveTrackFlow = global.moveTrackFlow
    val removeTrackFlow = global.removeTrackFlow

    fun play(track: Track) {
        viewModelScope.launch {
            tryWith(exceptionFlow) {
                val pos = global.addTrack(viewModelScope, track).first
                audioIndexFlow.emit(pos)
            }
        }
    }

    fun play(tracks: List<Track>, playIndex: Int? = null) {
        viewModelScope.launch {
            tryWith(exceptionFlow) {
                val pos = global.addTracks(viewModelScope, tracks).first
                playIndex?.let { audioIndexFlow.emit(pos + it) }
            }
        }
    }

    fun shuffle(shuffled: Boolean) {
        viewModelScope.launch {
            shuffle.emit(shuffled)
        }
    }

    fun addToQueue(track: Track) {
        viewModelScope.launch {
            tryWith(exceptionFlow) { global.addTrack(viewModelScope, track) }
        }
    }

    fun clearQueue() {
        global.clearQueue(viewModelScope)
    }

    fun moveQueueItems(new: Int, old: Int) {
        global.moveTrack(viewModelScope, old, new)
    }

    fun removeQueueItem(index: Int) {
        global.removeTrack(viewModelScope, index)
    }

    private val radioException = Exception(
        application.getString(
            R.string.is_not_supported,
            application.getString(R.string.radio)
        )
    )

    fun radio(artist: Artist.Full) {
        val client = trackClient
        if (client is RadioClient) {
            viewModelScope.launch(Dispatchers.IO) {
                val playlist = tryWith(exceptionFlow) { client.radio(artist) }
                    ?: return@launch
                play(playlist.tracks, 0)
            }
        } else viewModelScope.launch { exceptionFlow.emit(radioException) }
    }

    fun radio(album: Album.Full) {
        val client = trackClient
        if (client is RadioClient) {
            viewModelScope.launch(Dispatchers.IO) {
                val playlist = tryWith(exceptionFlow) { client.radio(album) }
                    ?: return@launch
                play(playlist.tracks, 0)
            }
        } else viewModelScope.launch { exceptionFlow.emit(radioException) }
    }

}