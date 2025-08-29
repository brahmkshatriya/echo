package dev.brahmkshatriya.echo.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

class FeedViewModel(
    val app: App,
    private val extensionLoader: ExtensionLoader,
) : ViewModel() {
    val feedDataMap = hashMapOf<String, FeedData>()
    fun getFeedData(
        id: String,
        buttons: Feed.Buttons = Feed.Buttons(),
        noVideos: Boolean = false,
        vararg extraLoadFlow: Flow<*>,
        cached: suspend ExtensionLoader.() -> FeedData.State<Feed<Shelf>>? = { null },
        loader: suspend ExtensionLoader.() -> FeedData.State<Feed<Shelf>>?
    ): FeedData {
        return feedDataMap.getOrPut(id) {
            FeedData(
                feedId = id,
                scope = viewModelScope,
                app = app,
                extensionLoader = extensionLoader,
                cached = cached,
                load = loader,
                defaultButtons = buttons,
                noVideos = noVideos,
                extraLoadFlow = extraLoadFlow.toList().merge(),
            )
        }
    }
}