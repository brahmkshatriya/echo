package dev.brahmkshatriya.echo.ui.search

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.ui.common.FeedViewModel
import dev.brahmkshatriya.echo.ui.paging.toFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    override val extensionFlow: MutableStateFlow<MusicExtension?>,
    override val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
    override val userFlow: MutableSharedFlow<UserEntity?>,
) : FeedViewModel(throwableFlow, userFlow, extensionFlow, extensionListFlow) {

    var query: String = ""
    override suspend fun getTabs(client: ExtensionClient) =
        (client as? SearchFeedClient)?.searchTabs(query)

    override fun getFeed(client: ExtensionClient) =
        (client as? SearchFeedClient)?.searchFeed(query, tab)?.toFlow()

    val quickFeed = MutableStateFlow<List<QuickSearchItem>>(emptyList())
    fun quickSearch(query: String) {
        val extension = extensionFlow.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val list = extension.get<SearchFeedClient, List<QuickSearchItem>>(throwableFlow) {
                quickSearch(query)
            } ?: emptyList()
            quickFeed.value = list
        }
    }

    fun deleteSearch(item: QuickSearchItem, query: String) {
        val extension = extensionFlow.value ?: return
        viewModelScope.launch {
            extension.get<SearchFeedClient, Unit>(throwableFlow) {
                deleteQuickSearch(item)
            }
            quickSearch(query)
        }
    }

}