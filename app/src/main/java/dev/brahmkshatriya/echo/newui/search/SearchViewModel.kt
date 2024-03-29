package dev.brahmkshatriya.echo.newui.search

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.newui.CatchingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionFlow: ExtensionModule.ExtensionFlow,
) : CatchingViewModel(throwableFlow) {

    val loading = MutableSharedFlow<Boolean>()
    val searchFeed = MutableStateFlow<PagingData<MediaItemsContainer>?>(null)
    val quickFeed = MutableStateFlow<List<QuickSearchItem>>(emptyList())
    val genres = MutableStateFlow<List<Genre>>(emptyList())
    var genre: Genre? = null
        set(value) {
            if (value != field) refresh()
            field = value
        }
    var query: String? = null

    override fun onInitialize() {
        viewModelScope.launch {
            extensionFlow.flow.collect {
                val client = it as? SearchClient ?: return@collect
                loadGenres(client)
            }
        }
    }

    private suspend fun loadGenres(client: SearchClient) {
        loading.emit(true)
        val list = tryWith { client.searchGenres(query) } ?: emptyList()
        loading.emit(false)
        genre = list.firstOrNull()
        genres.value = list
    }


    private suspend fun loadFeed(client: SearchClient) = tryWith {
        searchFeed.value = null
        client.search(query, genre).collectTo(searchFeed)
    }

    fun refresh(reset: Boolean = false) {
        val client = extensionFlow.flow.value as? SearchClient ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (reset) {
                genre = null
                loadGenres(client)
            }
            loadFeed(client)
        }
    }

    fun quickSearch(query: String) {
        val client = extensionFlow.flow.value as? SearchClient ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val list = tryWith { client.quickSearch(query) } ?: emptyList()
            quickFeed.value = list
        }
    }

}