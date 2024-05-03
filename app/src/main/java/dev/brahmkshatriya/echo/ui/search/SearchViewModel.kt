package dev.brahmkshatriya.echo.ui.search

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.ui.common.FeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    override val extensionFlow: ExtensionModule.ExtensionFlow,
    val extensionListFlow: ExtensionModule.ExtensionListFlow,
) : FeedViewModel<SearchClient>(throwableFlow, extensionFlow) {

    var query: String? = null
    override suspend fun getTabs(client: SearchClient) = client.searchGenres(query)
    override fun getFeed(client: SearchClient) = client.search(query, genre)

    val quickFeed = MutableStateFlow<List<QuickSearchItem>>(emptyList())
    fun quickSearch(query: String) {
        val client = extensionFlow.value as? SearchClient ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val list = tryWith { client.quickSearch(query) } ?: emptyList()
            quickFeed.value = list
        }
    }

}