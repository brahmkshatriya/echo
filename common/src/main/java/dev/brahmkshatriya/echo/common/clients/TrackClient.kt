package dev.brahmkshatriya.echo.common.clients

import android.net.Uri
import dev.brahmkshatriya.echo.common.models.Track

interface TrackClient {
    suspend fun getTrack(uri: Uri): Track?
}