package dev.brahmkshatriya.echo.data.clients

import android.net.Uri
import dev.brahmkshatriya.echo.data.models.StreamableAudio
import dev.brahmkshatriya.echo.data.models.Track

interface TrackClient {
    suspend fun getTrack(uri: Uri): Track?
    suspend fun getStreamable(track: Track): StreamableAudio

}