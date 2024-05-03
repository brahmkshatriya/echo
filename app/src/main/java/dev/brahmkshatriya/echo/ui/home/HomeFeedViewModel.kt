package dev.brahmkshatriya.echo.ui.home

import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.models.UserEntity
import dev.brahmkshatriya.echo.ui.common.FeedViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@HiltViewModel
class HomeFeedViewModel @Inject constructor(
    override val extensionFlow: ExtensionModule.ExtensionFlow,
    val userFlow: MutableSharedFlow<UserEntity?>,
    throwableFlow: MutableSharedFlow<Throwable>,
) : FeedViewModel<HomeFeedClient>(throwableFlow, extensionFlow) {
    override suspend fun getTabs(client: HomeFeedClient) = client.getHomeGenres()
    override fun getFeed(client: HomeFeedClient) = client.getHomeFeed(genre)

}