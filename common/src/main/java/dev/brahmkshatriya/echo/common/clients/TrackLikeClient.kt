package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Track

/**
 * Used to like or unlike a track.
 */
interface TrackLikeClient {
    /**
     * Likes or unlikes a track.
     *
     * @param track the track to like or unlike.
     * @param isLiked whether the track should be liked or unliked.
     */
    suspend fun likeTrack(track: Track, isLiked: Boolean)
}