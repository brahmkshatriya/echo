package dev.brahmkshatriya.echo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.data.clients.TrackClient
import dev.brahmkshatriya.echo.data.models.StreamableAudio
import dev.brahmkshatriya.echo.data.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val trackClient: TrackClient,
//    private val radioClient: RadioClient
) : ViewModel() {

    data class TrackWithStream(
        val track: Track,
        val audio: StreamableAudio
    )

    val playPause: MutableSharedFlow<Boolean> = MutableSharedFlow()

    val seekTo: MutableSharedFlow<Long> = MutableSharedFlow()
    val seekToPrevious: MutableSharedFlow<Unit> = MutableSharedFlow()
    val seekToNext: MutableSharedFlow<Unit> = MutableSharedFlow()

    val audioIndexFlow = MutableSharedFlow<Int>()
    val audioQueueFlow = MutableSharedFlow<TrackWithStream>()

    private suspend fun loadStreamable(track: Track) =
        TrackWithStream(track, trackClient.getStreamable(track))

    private val queue = mutableListOf<TrackWithStream>()
    private suspend fun loadAndAddToQueue(track: Track): Int {
        val stream = loadStreamable(track)
        queue.add(stream)
        audioQueueFlow.emit(stream)
        return queue.count() - 1
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

//    fun radio(track: Track){
//        viewModelScope.launch(Dispatchers.IO) {
//            val playlist = radioClient.radio(track)
//            playlist.tracks.forEach {
//                audioQueueFlow.value = loadStreamable(it)
//            }
//        }
//    }

}