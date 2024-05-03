package dev.brahmkshatriya.echo.playback

import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.utils.collect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Collections.synchronizedList

class Queue {
    val updateFlow = MutableSharedFlow<Unit>()

    private val trackQueue = synchronizedList(mutableListOf<StreamableTrack>())
    private val playerQueue = synchronizedList(mutableListOf<StreamableTrack>())
    val queue get() = playerQueue.toList()

    val currentIndexFlow = MutableStateFlow(-1)
    val current get() = queue.getOrNull(currentIndexFlow.value)
    val currentAudioFlow = MutableStateFlow<StreamableAudio?>(null)

    fun getTrack(mediaId: String?) = trackQueue.find { it.unloaded.id == mediaId }

    private val clearQueueFlow = MutableSharedFlow<Unit>()
    suspend fun clearQueue() {
        trackQueue.clear()
        playerQueue.clear()
        clearQueueFlow.emit(Unit)
    }

    private val removeTrackFlow = MutableSharedFlow<Int>()
    suspend fun removeTrack(index: Int) {
        val track = playerQueue[index]
        trackQueue.remove(track)
        removeTrackFlow.emit(index)
        if (trackQueue.isEmpty()) clearQueueFlow.emit(Unit)
    }

    private val addTrackFlow = MutableSharedFlow<Pair<Int, List<MediaItem>>>()
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
        addTrackFlow.emit(mediaItems)
        return mediaItems
    }

    private val moveTrackFlow = MutableSharedFlow<Pair<Int, Int>>()
    suspend fun moveTrack(fromIndex: Int, toIndex: Int) {
        moveTrackFlow.emit(fromIndex to toIndex)
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
        val onLoad: MutableSharedFlow<Track> = MutableSharedFlow(),
        val onLiked: MutableSharedFlow<Boolean> = MutableStateFlow(unloaded.liked),
    ) {
        val current get() = loaded ?: unloaded
    }

    suspend fun listenToChanges(
        session: MediaSession,
        updateLayout: (StreamableTrack) -> Unit
    ) = withContext(Dispatchers.Main) {
        val player = session.player
        collect(addTrackFlow) { (index, item) ->
            player.addMediaItems(index, item)
            player.prepare()
            player.playWhenReady = true
        }
        collect(moveTrackFlow) { (new, old) ->
            player.moveMediaItem(old, new)
        }
        collect(removeTrackFlow) {
            player.removeMediaItem(it)
        }
        collect(clearQueueFlow) {
            player.pause()
            player.clearMediaItems()
            player.stop()
        }
        collect(currentIndexFlow) {
            val track = current ?: return@collect
            updateLayout(track)
            collect(track.onLiked) { updateLayout(track) }
            val loaded = track.loaded ?: track.onLoad.first()
            val metadata = loaded.toMetaData()
            player.apply {
                val mediaItem = currentMediaItem ?: return@apply
                val newItem = mediaItem.buildUpon().setMediaMetadata(metadata).build()
                replaceMediaItem(currentMediaItemIndex, newItem)
            }
            updateLayout(track)
        }
    }
}