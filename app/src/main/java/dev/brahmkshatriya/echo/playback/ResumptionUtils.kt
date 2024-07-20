package dev.brahmkshatriya.echo.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.context
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.utils.getFromCache
import dev.brahmkshatriya.echo.utils.getListFromCache
import dev.brahmkshatriya.echo.utils.saveToCache

object ResumptionUtils {
    fun saveQueue(context: Context, currentIndex: Int, list: List<MediaItem>) {
        val tracks = list.map { it.track }
        val clients = list.map { it.clientId }
        val contexts = list.map { it.context }
        context.saveToCache("queue_tracks", tracks, "queue")
        context.saveToCache("queue_contexts", contexts, "queue")
        context.saveToCache("queue_clients", "queue") { parcel ->
            parcel.writeStringList(clients)
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

    private fun recoverQueue(context: Context): List<MediaItem>? {
        val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val tracks = context.getListFromCache<Track>(
            "queue_tracks", Track.creator, "queue"
        )
        val clientIds = context.getFromCache("queue_clients", "queue") {
            it.createStringArrayList()
        }
        val contexts = context.getListFromCache<EchoMediaItem>(
            "queue_contexts", EchoMediaItem.creator, "queue"
        )
        return tracks?.mapIndexedNotNull { index, track ->
            val clientId = clientIds?.getOrNull(index) ?: return@mapIndexedNotNull null
            val item = contexts?.getOrNull(index)
            MediaItemUtils.build(settings, track, clientId, item)
        } ?: return null
    }

    private fun recoverIndex(context: Context) =
        context.getFromCache("queue_index", "queue") { it.readInt() }

    private fun recoverPosition(context: Context) =
        context.getFromCache("position", "queue") { it.readLong() }

    @OptIn(UnstableApi::class)
    fun recoverPlaylist(context: Context): MediaSession.MediaItemsWithStartPosition {
        val items = recoverQueue(context) ?: emptyList()
        val index = recoverIndex(context) ?: C.INDEX_UNSET
        val position = recoverPosition(context) ?: 0L
        return MediaSession.MediaItemsWithStartPosition(items, index, position)
    }

}