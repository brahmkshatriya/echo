package dev.brahmkshatriya.echo.playback

import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder

@Suppress("unused")
@OptIn(UnstableApi::class)
class ShufflePlayer(
    private val player: ExoPlayer,
) : ForwardingPlayer(player) {

    init {
        player.setShuffleOrder(ShuffleOrder.UnshuffledShuffleOrder(0))
    }

    private fun getQueue() = (0 until mediaItemCount).map { player.getMediaItemAt(it) }

    private var isShuffled = false
    private var original = getQueue()

    override fun setShuffleModeEnabled(enabled: Boolean) {
        if (enabled) original = getQueue()
        isShuffled = enabled
        changeQueue(if (enabled) original.shuffled() else original)
        player.shuffleModeEnabled = enabled
    }

    override fun hasNextMediaItem(): Boolean {
        return currentMediaItemIndex < mediaItemCount - 1
    }

    @Suppress("UNUSED_PARAMETER")
    private fun log(name: String) {
//        println(name)
//        println("$isShuffled list ${original.size}: ${original.map { it.mediaMetadata.title }}")
//        println("player ${mediaItemCount}: ${getQueue().map { it.mediaMetadata.title }}")
    }

    private fun changeQueue(list: List<MediaItem>) {
        log("Change queue")
        if (list.size <= 1) return
        val currentMediaItem = player.currentMediaItem!!
        val index = list.find { it == currentMediaItem }?.let { list.indexOf(it) }!!
        val before = list.take(index) - currentMediaItem
        val after = list.takeLast(list.size - index) - currentMediaItem
        if (currentMediaItemIndex > 0) player.removeMediaItems(0, currentMediaItemIndex)
        player.addMediaItems(0, before)
        player.removeMediaItems(currentMediaItemIndex + 1, mediaItemCount)
        player.addMediaItems(currentMediaItemIndex + 1, after)
    }

    override fun addMediaItem(mediaItem: MediaItem) {
        if (isShuffled) original = original + mediaItem
        player.addMediaItem(mediaItem)
        log("Add media item")
    }

    override fun addMediaItems(mediaItems: MutableList<MediaItem>) {
        if (isShuffled) original = original + mediaItems
        player.addMediaItems(mediaItems)
        log("Add media items")
    }

    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
        if (isShuffled) original = original + mediaItem
        player.addMediaItem(index, mediaItem)
        log("Add media item at $index")
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        if (isShuffled) original = original + mediaItems
        player.addMediaItems(index, mediaItems)
        log("Add media items at $index")
    }

    override fun removeMediaItem(index: Int) {
        if (isShuffled) original = original - getMediaItemAt(index)
        player.removeMediaItem(index)
        log("Remove media item at $index")
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        if (isShuffled) original =
            original - (fromIndex until toIndex).map { getMediaItemAt(it) }.toSet()
        player.removeMediaItems(fromIndex, toIndex)
        log("Remove media items from $fromIndex to $toIndex")
    }

    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
        if (isShuffled) original = original.toMutableList().apply {
            val originalIndex = indexOf(getMediaItemAt(index))
            if (originalIndex != -1) set(originalIndex, mediaItem)
        }
        player.replaceMediaItem(index, mediaItem)
        log("Replace media item at $index")
    }

    override fun replaceMediaItems(
        fromIndex: Int, toIndex: Int, mediaItems: MutableList<MediaItem>
    ) {
        if (isShuffled) original = original.toMutableList().apply {
            val originalIndexes = (fromIndex until toIndex).map { indexOf(getMediaItemAt(it)) }
            originalIndexes.forEachIndexed { i, originalIndex ->
                if (originalIndex != -1) set(originalIndex, mediaItems[i])
            }
        }
        player.replaceMediaItems(fromIndex, toIndex, mediaItems)
        log("Replace media items from $fromIndex to $toIndex")
    }

    override fun setMediaItem(mediaItem: MediaItem) {
        if (isShuffled) original = listOf(mediaItem)
        player.setMediaItem(mediaItem)
        log("Set media item")
    }

    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {
        if (isShuffled) original = listOf(mediaItem)
        player.setMediaItem(mediaItem, resetPosition)
        log("Set media item")
    }

    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {
        if (isShuffled) original = listOf(mediaItem)
        player.setMediaItem(mediaItem, startPositionMs)
        log("Set media item")
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>) {
        if (isShuffled) original = mediaItems
        player.setMediaItems(mediaItems)
        log("Set media items")
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        if (isShuffled) original = mediaItems
        player.setMediaItems(mediaItems, resetPosition)
        log("Set media items")
    }

    override fun setMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ) {
        if (isShuffled) original = mediaItems
        player.setMediaItems(mediaItems, startIndex, startPositionMs)
        log("Set media items")
    }

    override fun clearMediaItems() {
        original = emptyList()
        player.clearMediaItems()
        log("Clear media items")
    }
}
