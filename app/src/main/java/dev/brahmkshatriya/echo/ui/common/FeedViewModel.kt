package dev.brahmkshatriya.echo.ui.common

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.plugger.echo.ExtensionInfo
import dev.brahmkshatriya.echo.plugger.echo.MusicExtension
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class FeedViewModel(
    throwableFlow: MutableSharedFlow<Throwable>,
    open val userFlow: MutableSharedFlow<UserEntity?>,
    open val extensionFlow: MutableStateFlow<MusicExtension?>,
    open val extensionListFlow: MutableStateFlow<List<MusicExtension>?>
) : CatchingViewModel(throwableFlow) {
    abstract suspend fun getTabs(client: ExtensionClient): List<Tab>?
    abstract fun getFeed(client: ExtensionClient): Flow<PagingData<Shelf>>?

    var recyclerPosition = 0

    val loading = MutableSharedFlow<Boolean>()
    val feed = MutableStateFlow<PagingData<Shelf>?>(null)
    val genres = MutableStateFlow<List<Tab>>(emptyList())
    var tab: Tab? = null

    override fun onInitialize() {
        viewModelScope.launch {
            extensionFlow.collect { refresh(true) }
        }
    }


    private suspend fun loadGenres(info: ExtensionInfo, client: ExtensionClient) {
        loading.emit(true)
        val list = tryWith(info) { getTabs(client) } ?: emptyList()
        loading.emit(false)
        if (!list.any { it.id == tab?.id }) tab = list.firstOrNull()
        genres.value = list
    }


    private suspend fun loadFeed(info: ExtensionInfo, client: ExtensionClient) = tryWith(info) {
        getFeed(client)?.collectTo(feed)
    }

    private var job: Job? = null
    fun refresh(reset: Boolean = false) {
        job?.cancel()
        feed.value = null
        val extension = extensionFlow.value ?: return
        val client = extension.client
        val info = extension.info
        job = viewModelScope.launch(Dispatchers.IO) {
            if (reset) loadGenres(info, client)
            loadFeed(info, client)
        }
    }
}