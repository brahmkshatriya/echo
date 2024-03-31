package dev.brahmkshatriya.echo.viewmodels

import androidx.lifecycle.ViewModel
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track

class PlayerViewModel : ViewModel() {
    fun play(clientId: String, track: Track) {
        println("TODO")
    }

    fun play(clientId: String, tracks: List<Track>) {
        println("TODO")
    }


    fun radio(clientId: String, album: Album) {
        println("TODO")
    }

    fun radio(clientId: String, artist: Artist) {
        println("TODO")
    }

    fun radio(clientId: String, playlist: Playlist) {
        println("TODO")
    }
}