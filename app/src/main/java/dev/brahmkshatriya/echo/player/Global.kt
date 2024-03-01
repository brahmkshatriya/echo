package dev.brahmkshatriya.echo.player

import androidx.media3.common.MediaItem
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Collections

object Global {
    private val _queue = mutableListOf<Pair<String, Track>>()
    val queue get() = _queue.toList()
    fun getTrack(mediaId: String?) = _queue.find { it.first == mediaId }?.second

    private val _clearQueue = MutableSharedFlow<Unit>()
    val clearQueueFlow = _clearQueue.asSharedFlow()
    fun clearQueue(scope: CoroutineScope) {
        scope.launch {
            _clearQueue.emit(Unit)
        }
        _queue.clear()
    }

    private val _removeTrack = MutableSharedFlow<Int>()
    val removeTrackFlow = _removeTrack.asSharedFlow()
    fun removeTrack(scope: CoroutineScope, index: Int) {
        _queue.removeAt(index)
        scope.launch {
            _removeTrack.emit(index)
            if (_queue.isEmpty()) _clearQueue.emit(Unit)
        }
    }

    private val _addTrack = MutableSharedFlow<Triple<Int, MediaItem, Track>>()
    val addTrackFlow = _addTrack.asSharedFlow()
    fun addTrack(
        scope: CoroutineScope, track: Track, stream: StreamableAudio, positionOffset: Int = 0
    ): Pair<Int, MediaItem> {
        val item = PlayerHelper.mediaItemBuilder(track, stream)
        val mediaId = item.mediaId
        val index = _queue.size - positionOffset

        _queue.add(index, mediaId to track)
        scope.launch {
            _addTrack.emit(Triple(index, item, track))
        }
        return index to item
    }

    private val _moveTrack = MutableSharedFlow<Pair<Int, Int>>()
    val moveTrackFlow = _moveTrack.asSharedFlow()
    fun moveTrack(scope: CoroutineScope, fromIndex: Int, toIndex: Int) {
        Collections.swap(_queue, fromIndex, toIndex)
        scope.launch {
            _moveTrack.emit(fromIndex to toIndex)
        }
    }
}