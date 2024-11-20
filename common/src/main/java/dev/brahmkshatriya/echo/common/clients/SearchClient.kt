package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab

interface SearchClient {
    suspend fun quickSearch(query: String?): List<QuickSearchItem>
    suspend fun deleteQuickSearch(item: QuickSearchItem)
    suspend fun searchTabs(query: String?): List<Tab>
    fun searchFeed(query: String?, tab: Tab?): PagedData<Shelf>
}