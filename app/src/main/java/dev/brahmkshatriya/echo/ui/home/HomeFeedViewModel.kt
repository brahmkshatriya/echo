package dev.brahmkshatriya.echo.ui.home

import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.models.UserEntity
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.ui.common.FeedViewModel
import dev.brahmkshatriya.echo.ui.paging.toFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeFeedViewModel @Inject constructor(
    override val extensionFlow: MutableStateFlow<MusicExtension?>,
    val userFlow: MutableSharedFlow<UserEntity?>,
    throwableFlow: MutableSharedFlow<Throwable>,
) : FeedViewModel(throwableFlow, extensionFlow) {
    override suspend fun getTabs(client: ExtensionClient) =
        (client as? HomeFeedClient)?.getHomeTabs()

    override fun getFeed(client: ExtensionClient) =
        (client as? HomeFeedClient)?.getHomeFeed(tab)?.toFlow()
}