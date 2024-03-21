package dev.brahmkshatriya.echo.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.utils.catchWith
import dev.brahmkshatriya.echo.utils.tryWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlaylistViewModel : ViewModel() {

    private var initialized = false

    private val _result: MutableStateFlow<PagingData<MediaItemsContainer>?> = MutableStateFlow(null)
    val result = _result.asStateFlow()
    private val mutablePlaylistFlow: MutableStateFlow<Playlist?> = MutableStateFlow(null)
    val playlistFlow = mutablePlaylistFlow.asStateFlow()

    fun loadAlbum(
        playlistClient: PlaylistClient, throwableFlow: MutableSharedFlow<Throwable>, playlist: Playlist
    ) {
        if (initialized) return
        initialized = true
        viewModelScope.launch(Dispatchers.IO) {
            tryWith(throwableFlow) {
                playlistClient.loadPlaylist(playlist).let {
                    mutablePlaylistFlow.value = it
                    playlistClient.getMediaItems(it).catchWith(throwableFlow).collectLatest { data ->
                        _result.value = data
                    }
                }
            }
        }
    }
}