package dev.brahmkshatriya.echo.ui.main.search

import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.ui.main.FeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchViewModel(
    app: App,
    extensionLoader: ExtensionLoader,
    extensionId: Result<String>
) : FeedViewModel(app.throwFlow, extensionLoader) {

    override val current = run {
        val id = extensionId.getOrNull()
        if (id == null) super.current
        else {
            MutableStateFlow<MusicExtension?>(null).also { state ->
                val music = extensionLoader.music
                viewModelScope.launch {
                    music.collectLatest {
                        state.value = music.getExtension(id)
                    }
                }
            }
        }
    }

    var query: String? = null

    override suspend fun getTabs(extension: Extension<*>) =
        extension.get<SearchFeedClient, List<Tab>> {
            searchTabs(query ?: "")
        }

    override suspend fun getFeed(extension: Extension<*>) =
        extension.get<SearchFeedClient, Feed> {
            searchFeed(query ?: "", tab)
        }

    val quickFeed = MutableStateFlow<List<QuickSearchItem>>(emptyList())
    fun quickSearch(query: String) {
        val extension = current.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val list = extension.get<SearchFeedClient, List<QuickSearchItem>>(throwableFlow) {
                quickSearch(query)
            } ?: emptyList()
            quickFeed.value = list
        }
    }

    fun deleteSearch(item: QuickSearchItem, query: String) {
        val extension = current.value ?: return
        viewModelScope.launch {
            extension.get<SearchFeedClient, Unit>(throwableFlow) {
                deleteQuickSearch(item)
            }
            quickSearch(query)
        }
    }

    init {
        init()
    }
}