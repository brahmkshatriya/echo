package dev.brahmkshatriya.echo.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.context
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.utils.getFromCache
import dev.brahmkshatriya.echo.utils.saveToCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ResumptionUtils {

    private const val FOLDER = "queue"
    private const val TRACKS = "queue_tracks"
    private const val CONTEXTS = "queue_contexts"
    private const val EXTENSIONS = "queue_extensions"
    private const val INDEX = "queue_index"
    private const val POSITION = "position"

    private fun Player.mediaItems() = (0 until mediaItemCount).map { getMediaItemAt(it) }
    suspend fun saveQueue(context: Context, player: Player) = runCatching {
        withContext(Dispatchers.IO) {
            val list = player.mediaItems()
            val currentIndex = player.currentMediaItemIndex
            val tracks = list.map { it.track }
            val extensionIds = list.map { it.extensionId }
            val contexts = list.map { it.context }
            context.saveToCache(TRACKS, tracks, FOLDER)
            context.saveToCache(CONTEXTS, contexts, FOLDER)
            context.saveToCache(EXTENSIONS, extensionIds, FOLDER)
            context.saveToCache(INDEX, currentIndex, FOLDER)
        }
    }

    fun saveCurrentPos(context: Context, position: Long) {
        context.saveToCache("position", position, FOLDER)
    }

    private fun Context.recoverQueue(): List<MediaItem>? {
        val settings = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val tracks = getFromCache<List<Track>>(TRACKS, FOLDER)
        val extensionIds = getFromCache<List<String>>(EXTENSIONS, FOLDER)
        val contexts = getFromCache<List<EchoMediaItem>>(CONTEXTS, FOLDER)
        return tracks?.mapIndexedNotNull { index, track ->
            val extensionId = extensionIds?.getOrNull(index) ?: return@mapIndexedNotNull null
            val item = contexts?.getOrNull(index)
            MediaItemUtils.build(settings, track, extensionId, item)
        } ?: return null
    }

    private fun Context.recoverIndex() = getFromCache<Int>(INDEX, FOLDER)
    private fun Context.recoverPosition() = getFromCache<Long>(POSITION, FOLDER)

    @OptIn(UnstableApi::class)
    fun recoverPlaylist(context: Context): MediaSession.MediaItemsWithStartPosition {
        val items = context.recoverQueue() ?: emptyList()
        val index = context.recoverIndex() ?: C.INDEX_UNSET
        val position = context.recoverPosition() ?: 0L
        return MediaSession.MediaItemsWithStartPosition(items, index, position)
    }

}