package dev.brahmkshatriya.echo.data.clients

import dev.brahmkshatriya.echo.data.models.Playlist
import dev.brahmkshatriya.echo.data.models.Track

interface TrackClient {
    suspend fun loadTrack(small: Track.Small): Track.Full
    suspend fun radio(track: Track.Small): Playlist.Full

}