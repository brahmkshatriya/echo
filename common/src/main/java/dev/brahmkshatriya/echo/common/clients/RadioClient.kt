package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User

interface RadioClient : TrackClient {
    fun loadTracks(radio: Radio): PagedData<Track>
    suspend fun radio(track: Track, context: EchoMediaItem?): Radio
    suspend fun radio(album: Album): Radio
    suspend fun radio(artist: Artist): Radio
    suspend fun radio(user: User): Radio
    suspend fun radio(playlist: Playlist): Radio
}