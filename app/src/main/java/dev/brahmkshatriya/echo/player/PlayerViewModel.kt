package dev.brahmkshatriya.echo.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.ExtensionFlow
import dev.brahmkshatriya.echo.utils.observe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    trackFlow: ExtensionFlow,
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
    val shuffle: MutableSharedFlow<List<Pair<Int, Int>>> = MutableSharedFlow()

    private suspend fun loadStreamable(track: Track): StreamableAudio? {
        return trackClient?.getStreamable(track) ?: return null
    }

    private suspend fun loadAndAddToQueue(track: Track): Int {
        val stream = loadStreamable(track)
        return stream?.let {
            Global.addTrack(viewModelScope, track, it).first
        } ?: -1
    }

    fun play(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            audioIndexFlow.emit(loadAndAddToQueue(track))
        }
    }

    fun play(tracks: List<Track>) {
        clearQueue()
        viewModelScope.launch(Dispatchers.IO) {
            tracks.forEachIndexed { index, track ->
                if (index == 0) audioIndexFlow.emit(loadAndAddToQueue(track))
                else loadAndAddToQueue(track)
            }
        }
    }

    private var oldList: List<Pair<Int, Int>>? = null
    fun shuffle(shuffled: Boolean) {
        println("Shuffling: $shuffled")
        val list = if (shuffled) {
            (0..<Global.queue.size).shuffled().mapIndexed { i, j -> i to j }
                .also { oldList = it.asReversed() }
        } else oldList ?: return
        println(list)
        viewModelScope.launch(Dispatchers.IO) {
            shuffle.emit(list)
        }
        list.forEach { (i, j) ->
            moveQueueItems(i, j)
        }
    }

    fun addToQueue(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            loadAndAddToQueue(track)
        }
    }

    fun clearQueue() {
        Global.clearQueue(viewModelScope)
    }

    fun moveQueueItems(new: Int, old: Int) {
        Global.moveTrack(viewModelScope, old, new)
    }

    fun removeQueueItem(index: Int) {
        Global.removeTrack(viewModelScope, index)
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