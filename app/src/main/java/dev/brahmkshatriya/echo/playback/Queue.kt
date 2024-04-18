package dev.brahmkshatriya.echo.playback

import androidx.media3.common.MediaItem
import dev.brahmkshatriya.echo.common.models.Track
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Collections.synchronizedList

class Queue {

    private val trackQueue = synchronizedList(mutableListOf<StreamableTrack>())
    private val playerQueue = synchronizedList(mutableListOf<StreamableTrack>())
    val queue get() = playerQueue.toList()

    val currentIndexFlow = MutableStateFlow(-1)
    val current get() = queue.getOrNull(currentIndexFlow.value)

    fun getTrack(mediaId: String?) = trackQueue.find { it.unloaded.id == mediaId }

    private val _clearQueue = MutableSharedFlow<Unit>()
    val clearQueueFlow = _clearQueue.asSharedFlow()
    suspend fun clearQueue() {
        trackQueue.clear()
        _clearQueue.emit(Unit)
    }

    private val _removeTrack = MutableSharedFlow<Int>()
    val removeTrackFlow = _removeTrack.asSharedFlow()
    suspend fun removeTrack(index: Int) {
        val track = playerQueue[index]
        trackQueue.remove(track)
        _removeTrack.emit(index)
        if (trackQueue.isEmpty()) _clearQueue.emit(Unit)
    }

    private val _addTrack = MutableSharedFlow<Pair<Int, List<MediaItem>>>()
    val addTrackFlow = _addTrack.asSharedFlow()
    suspend fun addTracks(
        client: String, tracks: List<Track>, offset: Int = 0
    ): Pair<Int, List<MediaItem>> {
        var position = currentIndexFlow.value + 1
        position += offset
        position = position.coerceIn(0, playerQueue.size)

        val items = tracks.map { track ->
            mediaItemBuilder(track)
        }
        val queueItems = tracks.map { track ->
            StreamableTrack(track, client)
        }
        trackQueue.addAll(queueItems)
        val mediaItems = position to items
        _addTrack.emit(mediaItems)
        return mediaItems
    }

    private val _moveTrack = MutableSharedFlow<Pair<Int, Int>>()
    val moveTrackFlow = _moveTrack.asSharedFlow()
    suspend fun moveTrack(fromIndex: Int, toIndex: Int) {
        _moveTrack.emit(fromIndex to toIndex)
    }

    fun updateQueue(mediaItems: List<String>) {
        val queue = mediaItems.mapNotNull { getTrack(it) }
        playerQueue.clear()
        playerQueue.addAll(queue)
    }

    data class StreamableTrack(
        val unloaded: Track,
        val clientId: String,
        var loaded: Track? = null,
        var liked: Boolean = unloaded.liked,
        val onLoad : MutableSharedFlow<Track> = MutableSharedFlow(),
        val onLiked: MutableSharedFlow<Boolean> = MutableSharedFlow(),

    )
}


