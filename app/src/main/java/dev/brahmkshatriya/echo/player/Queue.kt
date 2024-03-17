package dev.brahmkshatriya.echo.player

import androidx.media3.common.MediaItem
import dev.brahmkshatriya.echo.common.models.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.Collections.synchronizedList

class Queue {

    private val _queue = synchronizedList(mutableListOf<Pair<String, Track>>())
    val queue get() = synchronized(_queue) { _queue.toList() }

    val currentIndex = MutableStateFlow<Int?>(null)
    val current get() = currentIndex.value?.let { queue.getOrNull(it)?.second }

    fun getTrack(mediaId: String?) = queue.find { it.first == mediaId }?.second

    private val _clearQueue = MutableSharedFlow<Unit>()
    val clearQueueFlow = _clearQueue.asSharedFlow()
    fun clearQueue(scope: CoroutineScope) {
        _queue.clear()
        scope.launch {
            _clearQueue.emit(Unit)
        }
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

    private val _addTrack = MutableSharedFlow<Pair<Int, List<MediaItem>>>()
    val addTrackFlow = _addTrack.asSharedFlow()
    fun addTrack(
        scope: CoroutineScope, track: Track, offset: Int = 0
    ): Pair<Int, List<MediaItem>> {
        return addTracks(scope, listOf(track), offset)
    }

    fun addTracks(
        scope: CoroutineScope, tracks: List<Track>, offset: Int = 0
    ): Pair<Int, List<MediaItem>> {
        var position = currentIndex.value?.let { it + 1 } ?: 0
        position += offset
        position = position.coerceIn(0, _queue.size)

        val items = tracks.map { track ->
            val stream = track.stream ?: throw Exception("${track.title} is not streamable.")
            val item = PlayerHelper.mediaItemBuilder(track, stream)
            item
        }
        val queueItems = tracks.mapIndexed { index, track ->
            items[index].mediaId to track
        }
        _queue.addAll(position, queueItems)
        val mediaItems = position to items
        scope.launch {
            _addTrack.emit(mediaItems)
        }
        return mediaItems
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