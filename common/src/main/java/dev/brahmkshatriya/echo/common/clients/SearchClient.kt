package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Tab

interface SearchClient {
    suspend fun quickSearch(query: String?): List<QuickSearchItem>
    suspend fun deleteSearchHistory(query: QuickSearchItem.SearchQueryItem)
    suspend fun searchTabs(query: String?): List<Tab>
    fun searchFeed(query: String?, tab: Tab?): PagedData<MediaItemsContainer>
}