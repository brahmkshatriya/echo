package dev.brahmkshatriya.echo.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.utils.catchWith
import dev.brahmkshatriya.echo.utils.tryWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AlbumViewModel : ViewModel() {

    private var initialized = false

    private val _result: MutableStateFlow<PagingData<MediaItemsContainer>?> = MutableStateFlow(null)
    val result = _result.asStateFlow()
    private val mutableAlbumFlow: MutableStateFlow<Album.Full?> = MutableStateFlow(null)
    val albumFlow = mutableAlbumFlow.asStateFlow()

    fun loadAlbum(
        albumClient: AlbumClient, throwableFlow: MutableSharedFlow<Throwable>, album: Album.Small
    ) {
        if (initialized) return
        initialized = true
        viewModelScope.launch(Dispatchers.IO) {
            tryWith(throwableFlow) {
                albumClient.loadAlbum(album).let {
                    mutableAlbumFlow.value = it
                    albumClient.getMediaItems(it).catchWith(throwableFlow).collectLatest { data ->
                        _result.value = data
                    }
                }
            }
        }
    }
}