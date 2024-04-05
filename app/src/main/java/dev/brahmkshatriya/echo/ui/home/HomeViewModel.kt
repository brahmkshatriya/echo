package dev.brahmkshatriya.echo.ui.home

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionFlow: ExtensionModule.ExtensionFlow,
) : CatchingViewModel(throwableFlow) {

    val loading = MutableSharedFlow<Boolean>()
    val homeFeed = MutableStateFlow<PagingData<MediaItemsContainer>?>(null)
    val genres = MutableStateFlow<List<Genre>>(emptyList())
    var genre: Genre? = null
        set(newValue) {
            val oldValue = field
            field = newValue
            if (oldValue != newValue) refresh()
        }

    override fun onInitialize() {
        viewModelScope.launch {
            extensionFlow.collect {
                val client = it as? HomeFeedClient ?: return@collect
                loadGenres(client)
            }
        }
    }

    private suspend fun loadGenres(client: HomeFeedClient) {
        loading.emit(true)
        val list = tryWith { client.getHomeGenres() } ?: emptyList()
        loading.emit(false)
        genre = list.firstOrNull()
        genres.value = list
    }

    private suspend fun loadFeed(client: HomeFeedClient) = tryWith {
        homeFeed.value = null
        client.getHomeFeed(genre).collectTo(homeFeed)
    }

    fun refresh(reset: Boolean = false) {
        val client = extensionFlow.value as? HomeFeedClient ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (reset) {
                genre = null
                loadGenres(client)
            }
            loadFeed(client)
        }
    }
}