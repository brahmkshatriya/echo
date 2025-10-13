package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.EchoMediaItem

/**
 * Used to allow following and unfollowing an [EchoMediaItem]
 * with the [EchoMediaItem.isFollowable] set to true.
 *
 * @see MusicExtension
 */
interface FollowClient {

    /**
     * Checks if an item is followed.
     * This will only be called if the [EchoMediaItem.isFollowable] is true.
     *
     * @param item the item to check.
     * @return true if the item is followed, false otherwise.
     */
    suspend fun isFollowing(item: EchoMediaItem): Boolean

    /**
     * Gets the followers count of an item.
     * This will only be called if the [EchoMediaItem.isFollowable] is true.
     *
     * @param item the item to get the followers count of.
     * @return the number of followers or null if it cannot be determined.
     */
    suspend fun getFollowersCount(item: EchoMediaItem): Long?

    /**
     * Follows/Unfollow an Item.
     * This will only be called if the [EchoMediaItem.isFollowable] is true.
     *
     * @param item the item to follow.
     * @param shouldFollow whether to follow or unfollow the artist.
     */
    suspend fun followItem(item: EchoMediaItem, shouldFollow: Boolean)
}