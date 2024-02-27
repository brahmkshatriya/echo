package dev.brahmkshatriya.echo.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.models.Album
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumViewModel : ViewModel() {

    private var initialized = false

    private val mutableAlbumFlow: MutableStateFlow<Album.Full?> = MutableStateFlow(null)
    val albumFlow = mutableAlbumFlow.asStateFlow()

    fun loadAlbum(albumClient: AlbumClient, album: Album.Small) {
        if (initialized) return
        initialized = true
        viewModelScope.launch(Dispatchers.IO) {
            albumClient.loadAlbum(album).let {
                mutableAlbumFlow.value = it
            }
        }
    }
}