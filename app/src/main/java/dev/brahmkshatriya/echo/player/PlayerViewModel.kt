package dev.brahmkshatriya.echo.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.TrackFlow
import dev.brahmkshatriya.echo.utils.observe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    trackFlow: TrackFlow,
//    private val radioClient: RadioClient
) : ViewModel() {

    val fromNotification: MutableSharedFlow<Boolean> = MutableSharedFlow()

    private var trackClient: TrackClient? = null

    init {
        viewModelScope.observe(trackFlow.flow) {
            trackClient = it
        }
    }

    val audioIndexFlow = MutableSharedFlow<Int>()
    val audioQueueFlow = MutableSharedFlow<MediaItem>()
    val clearQueueFlow = MutableSharedFlow<Unit>()
    val itemMovedFlow = MutableSharedFlow<Pair<Int, Int>>()
    val itemRemovedFlow: MutableSharedFlow<Int> = MutableSharedFlow()

    val playPause: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val seekTo: MutableSharedFlow<Long> = MutableSharedFlow()
    val seekToPrevious: MutableSharedFlow<Unit> = MutableSharedFlow()
    val seekToNext: MutableSharedFlow<Unit> = MutableSharedFlow()
    val repeat: MutableSharedFlow<Int> = MutableSharedFlow()

    private suspend fun loadStreamable(track: Track): StreamableAudio? {
        return trackClient?.getStreamable(track) ?: return null
    }

    private val queue = Global.queue

    private suspend fun loadAndAddToQueue(track: Track): Int {
        val stream = loadStreamable(track)
        return stream?.let {
            val item = PlayerHelper.mediaItemBuilder(queue, track, it)
            audioQueueFlow.emit(item)
            queue.size - 1
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

    fun moveQueueItems(new: Int, old: Int) {
        Collections.swap(queue, new, old)
        viewModelScope.launch {
            itemMovedFlow.emit(new to old)
        }
    }

    fun removeQueueItem(index: Int) {
        queue.removeAt(index)
        viewModelScope.launch {
            if (queue.size == 0)
                clearQueueFlow.emit(Unit)
            else
                itemRemovedFlow.emit(index)
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