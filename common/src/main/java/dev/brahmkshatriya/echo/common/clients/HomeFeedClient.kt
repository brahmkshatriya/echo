package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab

interface HomeFeedClient {
    suspend fun getHomeTabs(): List<Tab>
    fun getHomeFeed(tab: Tab?): PagedData<Shelf>

}
