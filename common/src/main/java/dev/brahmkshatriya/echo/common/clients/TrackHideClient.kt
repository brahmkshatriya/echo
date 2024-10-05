package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Track

interface TrackHideClient {
    suspend fun hideTrack(track: Track, isHidden: Boolean)
}