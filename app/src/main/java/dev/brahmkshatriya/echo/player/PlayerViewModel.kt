package dev.brahmkshatriya.echo.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.TrackFlow
import dev.brahmkshatriya.echo.ui.utils.observe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    trackFlow: TrackFlow,
//    private val radioClient: RadioClient
) : ViewModel() {

    val fromNotification: MutableSharedFlow<Boolean> = MutableSharedFlow()

    private var trackClient: TrackClient? = null
    init {
        viewModelScope.observe(trackFlow.flow){
            trackClient = it
        }
    }

    data class TrackWithStream(
        val track: Track,
        val audio: StreamableAudio
    )

    val playPause: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val seekTo: MutableSharedFlow<Long> = MutableSharedFlow()
    val seekToPrevious: MutableSharedFlow<Unit> = MutableSharedFlow()
    val seekToNext: MutableSharedFlow<Unit> = MutableSharedFlow()
    val repeat: MutableSharedFlow<Int> = MutableSharedFlow()

    val audioIndexFlow = MutableSharedFlow<Int>()
    val audioQueueFlow = MutableSharedFlow<TrackWithStream>()
    val clearQueueFlow = MutableSharedFlow<Unit>()

    private suspend fun loadStreamable(track: Track): TrackWithStream? {
        val stream = trackClient?.getStreamable(track) ?: return null
        return TrackWithStream(track, stream)
    }
    private val queue = mutableListOf<TrackWithStream>()
    private suspend fun loadAndAddToQueue(track: Track): Int {
        val stream = loadStreamable(track)
        return stream?.let {
            queue.add(it)
            audioQueueFlow.emit(it)
            queue.count() - 1
        } ?: -1
    }

    fun play(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            audioIndexFlow.emit(loadAndAddToQueue(track))
        }
    }

    fun addToQueue(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            loadAndAddToQueue(track)
        }
    }

    fun clearQueue() {
        queue.clear()
        viewModelScope.launch {
            clearQueueFlow.emit(Unit)
        }
    }

//    fun radio(track: Track){
//        viewModelScope.launch(Dispatchers.IO) {
//            val playlist = radioClient.radio(track)
//            playlist.tracks.forEach {
//                audioQueueFlow.value = loadStreamable(it)
//            }
//        }
//    }

}