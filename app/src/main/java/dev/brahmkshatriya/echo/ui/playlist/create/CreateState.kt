package dev.brahmkshatriya.echo.ui.playlist.create

import dev.brahmkshatriya.echo.common.models.Playlist

sealed class CreateState {
    data object CreatePlaylist : CreateState()
    data object Creating : CreateState()
    data class PlaylistCreated(val extensionId: String, val playlist: Playlist?) : CreateState()
}