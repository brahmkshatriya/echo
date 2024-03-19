package dev.brahmkshatriya.echo.ui.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.UserClient
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.utils.tryWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArtistViewModel : ViewModel() {

    private var initialized = false

    private val _result: MutableStateFlow<PagingData<MediaItemsContainer>?> = MutableStateFlow(null)
    val result = _result.asStateFlow()
    private val mutableArtistFlow: MutableStateFlow<Artist.Full?> = MutableStateFlow(null)
    val artistFlow = mutableArtistFlow.asStateFlow()

    fun loadArtist(
        artistClient: ArtistClient,
        throwableFlow: MutableSharedFlow<Throwable>,
        artist: Artist.Small
    ) {
        if (initialized) return
        initialized = true
        viewModelScope.launch(Dispatchers.IO) {
            tryWith(throwableFlow) {
                artistClient.loadArtist(artist).let {
                    mutableArtistFlow.value = it
                    artistClient.getMediaItems(it).collectLatest { data ->
                        _result.value = data
                    }
                }
            }
        }
    }

    fun subscribe(
        userClient: UserClient,
        artist: Artist.Full,
        throwableFlow: MutableSharedFlow<Throwable>,
        subscribe: Boolean,
        block: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            tryWith(throwableFlow) {
                if(subscribe) userClient.subscribe(artist)
                else userClient.unsubscribe(artist)
            }
            withContext(Dispatchers.Main) {
                block(subscribe)
            }
        }
    }
}