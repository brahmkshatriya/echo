package dev.brahmkshatriya.echo.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.utils.collect
import dev.brahmkshatriya.echo.utils.getFromCache
import dev.brahmkshatriya.echo.utils.getListFromCache
import dev.brahmkshatriya.echo.utils.saveToCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Collections.synchronizedList

class Queue {
    val updateFlow = MutableSharedFlow<Unit>()

    private val trackQueue = synchronizedList(mutableListOf<StreamableTrack>())
    private val playerQueue = synchronizedList(mutableListOf<StreamableTrack>())
    val queue get() = playerQueue.toList()

    var currentIndex = -1
    val currentIndexFlow = MutableSharedFlow<Int>()
    val current get() = playerQueue.getOrNull(currentIndex)
    val currentAudioFlow = MutableStateFlow<StreamableAudio?>(null)

    fun getTrack(mediaId: String?) = trackQueue.find { it.unloaded.id == mediaId }

    private val clearQueueFlow = MutableSharedFlow<Unit>()
    suspend fun clearQueue(emit: Boolean = true) {
        trackQueue.clear()
        playerQueue.clear()
        if (emit) clearQueueFlow.emit(Unit)
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
        client: String, context: EchoMediaItem?, tracks: List<Track>, offset: Int = 0
    ): Pair<Int, List<MediaItem>> {
        var position = currentIndex + 1
        position += offset
        position = position.coerceIn(0, playerQueue.size)

        val items = tracks.map { track ->
            mediaItemBuilder(track)
        }
        val queueItems = tracks.map { track ->
            StreamableTrack(track, client, context)
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
        val context: EchoMediaItem? = null,
        var loaded: Track? = null,
        var liked: Boolean = unloaded.liked,
        val onLoad: MutableSharedFlow<Track> = MutableSharedFlow(),
    ) {
        val current get() = loaded ?: unloaded
    }

    val repeat = MutableStateFlow(REPEAT_MODE_OFF)
    val shuffle = MutableStateFlow(false)
    val onLiked = MutableSharedFlow<Boolean>()

    fun listenToChanges(
        scope: CoroutineScope,
        session: MediaSession,
        updateLayout: () -> Unit
    ) = scope.launch {
        val player = session.player
        collect(addTrackFlow) { (index, item) ->
            player.addMediaItems(index, item)
            player.prepare()
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
            updateLayout()
            val loaded = track.loaded ?: track.onLoad.first()
            val metadata = loaded.toMetaData()
            player.apply {
                val mediaItem = currentMediaItem ?: return@apply
                val newItem = mediaItem.buildUpon().setMediaMetadata(metadata).build()
                replaceMediaItem(currentMediaItemIndex, newItem)
            }
            updateLayout()
        }
        collect(onLiked) {
            updateLayout()
        }
        collect(repeat) {
            player.repeatMode = it
            updateLayout()
        }
        collect(shuffle) {
            player.shuffleModeEnabled = it
            updateLayout()
        }
    }

    fun saveQueue(context: Context, currentIndex: Int) {
        context.saveToCache("queue_tracks", trackQueue.map { it.unloaded }, "queue")
        context.saveToCache("queue_clients", "queue") { parcel ->
            parcel.writeStringList(trackQueue.map { it.clientId })
        }
        context.saveToCache("queue_index", "queue") {
            it.writeInt(currentIndex)
        }
    }

    fun saveCurrentPos(context: Context, position: Long) {
        context.saveToCache("position", "queue") {
            it.writeLong(position)
        }
    }

    private fun recoverQueue(context: Context): List<StreamableTrack>? {
        val queue = context.getListFromCache<Track>("queue_tracks", Track.creator, "queue")
        val clientIds = context.getFromCache("queue_clients", "queue") {
            it.createStringArrayList()
        }
        val streamableTracks = queue?.mapIndexedNotNull { index, track ->
            val clientId = clientIds?.getOrNull(index) ?: return@mapIndexedNotNull null
            StreamableTrack(track, clientId)
        } ?: return null
        trackQueue.addAll(streamableTracks)
        return streamableTracks
    }

    private fun recoverIndex(context: Context) =
        context.getFromCache("queue_index", "queue") { it.readInt() }

    private fun recoverPosition(context: Context) =
        context.getFromCache("position", "queue") { it.readLong() }

    @OptIn(UnstableApi::class)
    fun recoverPlaylist(context: Context): MediaSession.MediaItemsWithStartPosition {
        val recoveredTracks = recoverQueue(context) ?: emptyList()
        val index = recoverIndex(context) ?: C.INDEX_UNSET
        val position = recoverPosition(context) ?: 0L
        val items = recoveredTracks.map { mediaItemBuilder(it.unloaded) }
        return MediaSession.MediaItemsWithStartPosition(items, index, position)
    }
}