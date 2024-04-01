package dev.brahmkshatriya.echo.player

import androidx.media3.common.MediaItem
import dev.brahmkshatriya.echo.common.models.Track
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Collections
import java.util.Collections.synchronizedList

class Queue {

    private val _queue = synchronizedList(mutableListOf<StreamableTrack>())
    val queue get() = synchronized(_queue) { _queue.toList() }

    val currentIndex = MutableStateFlow<Int?>(null)
    val current get() = currentIndex.value?.let { queue.getOrNull(it) }

    fun getTrack(mediaId: String?) = queue.find { it.track.id == mediaId }

    private val _clearQueue = MutableSharedFlow<Unit>()
    val clearQueueFlow = _clearQueue.asSharedFlow()
    suspend fun clearQueue() {
        _queue.clear()
        _clearQueue.emit(Unit)
    }

    private val _removeTrack = MutableSharedFlow<Int>()
    val removeTrackFlow = _removeTrack.asSharedFlow()
    suspend fun removeTrack(index: Int) {
        _queue.removeAt(index)
        _removeTrack.emit(index)
        if (_queue.isEmpty()) _clearQueue.emit(Unit)
    }

    private val _addTrack = MutableSharedFlow<Pair<Int, List<MediaItem>>>()
    val addTrackFlow = _addTrack.asSharedFlow()
    suspend fun addTrack(
        client: String, track: Track, offset: Int = 0
    ): Pair<Int, List<MediaItem>> {
        return addTracks(client, listOf(track), offset)
    }

    suspend fun addTracks(
        client: String, tracks: List<Track>, offset: Int = 0
    ): Pair<Int, List<MediaItem>> {
        var position = currentIndex.value?.let { it + 1 } ?: 0
        position += offset
        position = position.coerceIn(0, _queue.size)

        val items = tracks.map { track ->
            PlayerHelper.mediaItemBuilder(track)
        }
        val queueItems = tracks.map { track ->
            StreamableTrack(track, client)
        }
        _queue.addAll(position, queueItems)
        val mediaItems = position to items
        _addTrack.emit(mediaItems)
        return mediaItems
    }


    private val _moveTrack = MutableSharedFlow<Pair<Int, Int>>()
    val moveTrackFlow = _moveTrack.asSharedFlow()
    suspend fun moveTrack(fromIndex: Int, toIndex: Int) {
        Collections.swap(_queue, fromIndex, toIndex)
        _moveTrack.emit(fromIndex to toIndex)
    }
}

data class StreamableTrack(
    val track: Track,
    val clientId: String
)
