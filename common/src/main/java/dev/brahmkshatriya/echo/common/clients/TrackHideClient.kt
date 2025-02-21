package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Track

/**
 * Used to hide the track from the user's feed.
 */
interface TrackHideClient {

    /**
     * Hides or unhides a track from the user's feed.
     *
     * @param track the track to hide or unhide.
     * @param isHidden whether the track should be hidden or unhidden.
     */
    suspend fun hideTrack(track: Track, isHidden: Boolean)
}