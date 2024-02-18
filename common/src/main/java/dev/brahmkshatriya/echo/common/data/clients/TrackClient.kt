package dev.brahmkshatriya.echo.common.data.clients

import android.net.Uri
import dev.brahmkshatriya.echo.common.data.models.StreamableAudio
import dev.brahmkshatriya.echo.common.data.models.Track

interface TrackClient {
    suspend fun getTrack(uri: Uri): Track?
    suspend fun getStreamable(track: Track): StreamableAudio

}