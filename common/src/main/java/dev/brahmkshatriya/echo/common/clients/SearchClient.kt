package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.QuickSearch
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab

interface SearchClient {
    suspend fun quickSearch(query: String?): List<QuickSearch>
    suspend fun deleteSearchHistory(query: QuickSearch.QueryItem)
    suspend fun searchTabs(query: String?): List<Tab>
    fun searchFeed(query: String?, tab: Tab?): PagedData<Shelf>
}