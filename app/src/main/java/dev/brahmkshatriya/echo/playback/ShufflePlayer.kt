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
        val currentMediaItem = list.first { it.mediaId == currentMediaItem?.mediaId }
        val index = list.indexOf(currentMediaItem)
        val before = list.take(index) - currentMediaItem
        val after = list.takeLast(list.size - index) - currentMediaItem
        if (currentMediaItemIndex > 0) player.removeMediaItems(0, currentMediaItemIndex)
        player.addMediaItems(0, before)
        player.removeMediaItems(currentMediaItemIndex + 1, mediaItemCount)
        player.addMediaItems(currentMediaItemIndex + 1, after)
    }

    fun onMediaItemChanged(old: MediaItem, new: MediaItem) {
        original = original.toMutableList().apply {
            val index = indexOf(old).takeIf { it != -1 } ?: return
            set(index, new)
        }
        log("Change media item")
    }

    override fun addMediaItem(mediaItem: MediaItem) {
        original = original + mediaItem
        player.addMediaItem(mediaItem)
        log("Add media item")
    }

    override fun addMediaItems(mediaItems: MutableList<MediaItem>) {
        original = original + mediaItems
        player.addMediaItems(mediaItems)
        log("Add media items")
    }

    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
        original = original + mediaItem
        player.addMediaItem(index, mediaItem)
        log("Add media item at $index")
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        original = original + mediaItems
        player.addMediaItems(index, mediaItems)
        log("Add media items at $index")
    }

    private fun getItemAt(index: Int) = player.getMediaItemAt(index).let {
        original.first { item -> item.mediaId == it.mediaId }
    }

    override fun removeMediaItem(index: Int) {
        original = original - getItemAt(index)
        player.removeMediaItem(index)
        log("Remove media item at $index")
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        original =
            original - (fromIndex until toIndex).map { getItemAt(it) }.toSet()
        player.removeMediaItems(fromIndex, toIndex)
        log("Remove media items from $fromIndex to $toIndex")
    }

    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
        original = original.toMutableList().apply {
            val originalIndex = indexOf(getItemAt(index)).takeIf { it != -1 }!!
            set(originalIndex, mediaItem)
        }
        player.replaceMediaItem(index, mediaItem)
        log("Replace media item at $index")
    }

    override fun replaceMediaItems(
        fromIndex: Int, toIndex: Int, mediaItems: MutableList<MediaItem>
    ) {
        original = original.toMutableList().apply {
            val originalIndexes = (fromIndex until toIndex).map { i ->
                indexOf(getItemAt(i)).takeIf { it != -1 }!!
            }
            originalIndexes.forEachIndexed { i, originalIndex ->
                set(originalIndex, mediaItems[i])
            }
        }
        player.replaceMediaItems(fromIndex, toIndex, mediaItems)
        log("Replace media items from $fromIndex to $toIndex")
    }

    override fun setMediaItem(mediaItem: MediaItem) {
        original = listOf(mediaItem)
        player.setMediaItem(mediaItem)
        log("Set media item")
    }

    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {
        original = listOf(mediaItem)
        player.setMediaItem(mediaItem, resetPosition)
        log("Set media item")
    }

    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {
        original = listOf(mediaItem)
        player.setMediaItem(mediaItem, startPositionMs)
        log("Set media item")
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>) {
        original = mediaItems
        player.setMediaItems(mediaItems)
        log("Set media items")
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        original = mediaItems
        player.setMediaItems(mediaItems, resetPosition)
        log("Set media items")
    }

    override fun setMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ) {
        original = mediaItems
        player.setMediaItems(
            mediaItems,
            startIndex.coerceAtMost(mediaItems.size - 1),
            startPositionMs
        )
        log("Set media items")
    }

    override fun clearMediaItems() {
        original = emptyList()
        player.clearMediaItems()
        log("Clear media items")
    }

}
