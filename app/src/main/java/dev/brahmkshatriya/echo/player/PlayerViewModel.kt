package dev.brahmkshatriya.echo.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.StreamableAudio
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
    private val exceptionFlow: MutableSharedFlow<Exception>
//    private val radioClient: RadioClient
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

    private suspend fun loadStreamable(track: Track): StreamableAudio? {
        return tryWith(exceptionFlow){ trackClient?.getStreamable(track) }
    }

    private suspend fun loadAndAddToQueue(track: Track): Int {
        val stream = loadStreamable(track)
        return stream?.let {
            global.addTrack(viewModelScope, track, it).first
        } ?: -1
    }

    fun play(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            audioIndexFlow.emit(loadAndAddToQueue(track))
        }
    }

    fun play(tracks: List<Track>) {
        viewModelScope.launch(Dispatchers.IO) {
            tracks.forEachIndexed { index, track ->
                if (index == 0) audioIndexFlow.emit(loadAndAddToQueue(track))
                else loadAndAddToQueue(track)
            }
        }
    }

    fun shuffle(shuffled: Boolean) {
        viewModelScope.launch{
            shuffle.emit(shuffled)
        }
    }

    fun addToQueue(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            loadAndAddToQueue(track)
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

//    fun radio(track: Track){
//        viewModelScope.launch(Dispatchers.IO) {
//            val playlist = radioClient.radio(track)
//            playlist.tracks.forEach {
//                audioQueueFlow.value = loadStreamable(it)
//            }
//        }
//    }

}