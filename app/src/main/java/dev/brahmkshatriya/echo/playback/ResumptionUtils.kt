package dev.brahmkshatriya.echo.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.playback.MediaItemUtils.context
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.utils.CacheUtils.getFromCache
import dev.brahmkshatriya.echo.utils.CacheUtils.saveToCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ResumptionUtils {

    private const val CLEARED = "cleared"
    private const val FOLDER = "queue"
    private const val TRACKS = "queue_tracks"
    private const val CONTEXTS = "queue_contexts"
    private const val EXTENSIONS = "queue_extensions"
    private const val INDEX = "queue_index"
    private const val POSITION = "position"
    private const val SHUFFLE = "shuffle"
    private const val REPEAT = "repeat"

    private fun Player.mediaItems() = (0 until mediaItemCount).map { getMediaItemAt(it) }
    fun saveIndex(context: Context, index: Int) {
        context.saveToCache(INDEX, index, FOLDER)
    }

    suspend fun saveQueue(context: Context, player: Player) = withContext(Dispatchers.Main) {
        val list = player.mediaItems()
        context.saveToCache(CLEARED, list.isEmpty())
        if (list.isEmpty()) return@withContext
        val currentIndex = player.currentMediaItemIndex
        withContext(Dispatchers.IO) {
            val extensionIds = list.map { it.extensionId }
            val tracks = list.map { it.track }
            val contexts = list.map { it.context }
            context.saveToCache(INDEX, currentIndex, FOLDER)
            context.saveToCache(EXTENSIONS, extensionIds, FOLDER)
            context.saveToCache(TRACKS, tracks, FOLDER)
            context.saveToCache(CONTEXTS, contexts, FOLDER)
        }
    }

    fun saveCurrentPos(context: Context, position: Long) {
        context.saveToCache(POSITION, position, FOLDER)
    }

    private suspend fun Context.recoverQueue(
        downloads: List<Downloader.Info>,
        withClear: Boolean = false
    ): List<MediaItem>? {
        if (withClear && getFromCache<Boolean>(CLEARED) != false) return null
        val tracks = getFromCache<List<Track>>(TRACKS, FOLDER)
        val extensionIds = getFromCache<List<String>>(EXTENSIONS, FOLDER)
        val contexts = getFromCache<List<EchoMediaItem>>(CONTEXTS, FOLDER)
        return tracks?.mapIndexedNotNull { index, track ->
            val extensionId = extensionIds?.getOrNull(index) ?: return@mapIndexedNotNull null
            val item = contexts?.getOrNull(index)
            MediaItemUtils.build(this, downloads, track, extensionId, item)
        } ?: return null
    }

    private fun Context.recoverIndex() = getFromCache<Int>(INDEX, FOLDER)
    private fun Context.recoverPosition() = getFromCache<Long>(POSITION, FOLDER)


    fun Context.recoverShuffle() = getFromCache<Boolean>(SHUFFLE, FOLDER)
    fun saveShuffle(context: Context, shuffle: Boolean) {
        context.saveToCache(SHUFFLE, shuffle, FOLDER)
    }

    fun Context.recoverRepeat() = getFromCache<Int>(REPEAT, FOLDER)
    fun saveRepeat(context: Context, repeat: Int) {
        context.saveToCache(REPEAT, repeat, FOLDER)
    }

    @OptIn(UnstableApi::class)
    suspend fun Context.recoverPlaylist(
        downloads: List<Downloader.Info>,
        withClear: Boolean = false
    ): Triple<List<MediaItem>, Int, Long> {
        val items = recoverQueue(downloads, withClear) ?: emptyList()
        val index = recoverIndex() ?: C.INDEX_UNSET
        val position = recoverPosition() ?: 0L
        return Triple(items, index, position)
    }

}