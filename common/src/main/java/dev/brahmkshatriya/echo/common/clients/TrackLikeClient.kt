package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Track

interface TrackLikeClient {
    suspend fun likeTrack(track: Track, isLiked: Boolean)
}