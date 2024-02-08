package dev.brahmkshatriya.echo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.data.clients.TrackClient
import dev.brahmkshatriya.echo.data.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val trackClient: TrackClient,
//    private val radioClient: RadioClient
) : ViewModel() {

    val audioFlow = MutableStateFlow<TrackWithStream?>(null)
    val audioQueueFlow = MutableStateFlow<TrackWithStream?>(null)

    private suspend fun loadStreamable(track: Track) =
        TrackWithStream(track, trackClient.getStreamable(track))

    fun play(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            val stream = loadStreamable(track)
            audioQueueFlow.value = stream
            delay(10)
            audioFlow.value = stream
        }
    }

    fun addToQueue(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            audioQueueFlow.value = loadStreamable(track)
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