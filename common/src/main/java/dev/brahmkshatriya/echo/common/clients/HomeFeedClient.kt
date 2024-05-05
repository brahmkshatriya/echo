package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Tab

interface HomeFeedClient {
    suspend fun getHomeTabs(): List<Tab>
    fun getHomeFeed(tab: Tab?): PagedData<MediaItemsContainer>

}
