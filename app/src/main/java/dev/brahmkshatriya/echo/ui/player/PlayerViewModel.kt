package dev.brahmkshatriya.echo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.data.clients.TrackClient
import dev.brahmkshatriya.echo.data.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val trackClient: TrackClient,
//    private val radioClient: RadioClient
) : ViewModel() {

    val audioIndexFlow = MutableStateFlow<Int?>(null)
    val audioQueueFlow = MutableStateFlow<TrackWithStream?>(null)

    private suspend fun loadStreamable(track: Track) =
        TrackWithStream(track, trackClient.getStreamable(track))


    val queue = mutableListOf<TrackWithStream>()
    private suspend fun loadAndAddToQueue(track: Track): Int {
        val stream = loadStreamable(track)
        queue.add(stream)
        audioQueueFlow.value = stream
        return queue.count() - 1
    }

    fun play(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            audioIndexFlow.value = loadAndAddToQueue(track)
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