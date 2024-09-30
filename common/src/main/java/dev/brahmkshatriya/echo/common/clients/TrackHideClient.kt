package dev.brahmkshatriya.echo.common.clients

interface TrackHideClient {
    suspend fun hideTrack(trackId: String, isHidden: Boolean)
}