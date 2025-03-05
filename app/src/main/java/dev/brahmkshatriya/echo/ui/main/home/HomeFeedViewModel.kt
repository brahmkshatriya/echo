package dev.brahmkshatriya.echo.ui.main.home

import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.ui.main.FeedViewModel

class HomeFeedViewModel(
    app: App,
    extensionLoader: ExtensionLoader,
) : FeedViewModel(app.throwFlow, extensionLoader) {

    override suspend fun getTabs(extension: Extension<*>) =
        extension.get<HomeFeedClient, List<Tab>> {
            getHomeTabs()
        }

    override suspend fun getFeed(extension: Extension<*>) =
        extension.get<HomeFeedClient, PagedData<Shelf>> {
            getHomeFeed(tab)
        }
}