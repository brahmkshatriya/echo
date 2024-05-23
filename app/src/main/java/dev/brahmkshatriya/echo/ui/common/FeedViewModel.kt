package dev.brahmkshatriya.echo.ui.common

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class FeedViewModel(
    throwableFlow: MutableSharedFlow<Throwable>,
    open val extensionFlow: MutableStateFlow<MusicExtension?>,
) : CatchingViewModel(throwableFlow) {
    abstract suspend fun getTabs(client: ExtensionClient): List<Tab>?
    abstract fun getFeed(client: ExtensionClient): Flow<PagingData<MediaItemsContainer>>?

    var recyclerPosition = 0

    val loading = MutableSharedFlow<Boolean>()
    val feed = MutableStateFlow<PagingData<MediaItemsContainer>?>(null)
    val genres = MutableStateFlow<List<Tab>>(emptyList())
    var tab: Tab? = null

    override fun onInitialize() {
        viewModelScope.launch {
            extensionFlow.collect { refresh(true) }
        }
    }


    private suspend fun loadGenres(client: ExtensionClient) {
        loading.emit(true)
        val list = tryWith { getTabs(client) } ?: emptyList()
        loading.emit(false)
        if (!list.any { it.id == tab?.id }) tab = list.firstOrNull()
        genres.value = list
    }


    private suspend fun loadFeed(client: ExtensionClient) = tryWith {
        getFeed(client)?.collectTo(feed)
    }

    fun refresh(reset: Boolean = false) {
        feed.value = null
        val client = extensionFlow.value?.client ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (reset) loadGenres(client)
            loadFeed(client)
        }
    }
}