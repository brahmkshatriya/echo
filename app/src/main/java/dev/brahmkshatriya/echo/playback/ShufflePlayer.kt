package dev.brahmkshatriya.echo.playback

import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track

@Suppress("unused")
@OptIn(UnstableApi::class)
// I couldn't make it work properly
// The only problem is when stuff is added while the shuffle is going, it fucks up everything
// aka, when shuffle is turned on with auto start radio, it fucks up everything
// feel free to make a pr to fix this
class ShufflePlayer(
    val player: Player,
) : ForwardingPlayer(player) {

    private fun getQueue() = (0 until mediaItemCount).map { player.getMediaItemAt(it) }

    private var isShuffled = false
    private var original = getQueue()

    override fun getShuffleModeEnabled() = isShuffled
    override fun setShuffleModeEnabled(enabled: Boolean) {
        if (enabled) original = getQueue()
        isShuffled = enabled
        changeQueue(if (enabled) original.shuffled() else original)
        player.shuffleModeEnabled = enabled
    }

    override fun hasNextMediaItem(): Boolean {
        return currentMediaItemIndex < mediaItemCount - 1
    }

    private fun print(bruh:String) {
        println(bruh)
        println("$isShuffled list ${original.size}: ${original.map { it.track.title }}")
        println("player ${mediaItemCount}: ${getQueue().map { it.track.title }}")
    }

    private fun changeQueue(list: List<MediaItem>) {
        print("Change queue")
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
        print("Add media item")
    }

    override fun addMediaItems(mediaItems: MutableList<MediaItem>) {
        if (isShuffled) original = original + mediaItems
        player.addMediaItems(mediaItems)
        print("Add media items")
    }

    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
        if (isShuffled) original = original + mediaItem
        player.addMediaItem(index, mediaItem)
        print("Add media item at $index")
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        if (isShuffled) original = original + mediaItems
        player.addMediaItems(index, mediaItems)
        print("Add media items at $index")
    }

    override fun removeMediaItem(index: Int) {
        if (isShuffled) original = original - getMediaItemAt(index)
        player.removeMediaItem(index)
        print("Remove media item at $index")
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        if (isShuffled) original =
            original - (fromIndex until toIndex).map { getMediaItemAt(it) }.toSet()
        player.removeMediaItems(fromIndex, toIndex)
        print("Remove media items from $fromIndex to $toIndex")
    }

    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
        if (isShuffled) original = original - getMediaItemAt(index) + mediaItem
        player.replaceMediaItem(index, mediaItem)
        print("Replace media item at $index")
    }

    override fun replaceMediaItems(
        fromIndex: Int, toIndex: Int, mediaItems: MutableList<MediaItem>
    ) {
        if (isShuffled) original =
            original - (fromIndex until toIndex).map { getMediaItemAt(it) }.toSet() + mediaItems
        player.replaceMediaItems(fromIndex, toIndex, mediaItems)
        print("Replace media items from $fromIndex to $toIndex")
    }

}
