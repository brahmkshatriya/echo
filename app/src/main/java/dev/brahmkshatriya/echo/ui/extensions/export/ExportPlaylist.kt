package dev.brahmkshatriya.echo.ui.extensions.export

import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Feed.Companion.loadAll
import dev.brahmkshatriya.echo.utils.Serializer.toData
import dev.brahmkshatriya.echo.utils.Serializer.toJson
import kotlinx.serialization.Serializable

@Serializable
data class ExportPlaylist(
    val title: String,
    val description: String? = null,
    val tracks: List<Track>,
)

class ExportPlaylistUtils {
    companion object {
        suspend fun PlaylistEditClient.serializePlaylists(): String {
            val playlists = this.listEditablePlaylists(null)
            val exportPlaylists = playlists.map { ExportPlaylist(
                it.first.title,
                it.first.description,
                this.loadTracks(it.first).loadAll(),
            ) }
            return exportPlaylists.toJson()
        }

        suspend fun PlaylistEditClient.deserializePlaylists(playlistJson: String) {
            val exportPlaylists = playlistJson.toData<List<ExportPlaylist>>()
            exportPlaylists.forEach {
                val playlist = this.createPlaylist(it.title, it.description)
                this.addTracksToPlaylist(playlist, emptyList(), 0, it.tracks)
            }
        }
    }
}