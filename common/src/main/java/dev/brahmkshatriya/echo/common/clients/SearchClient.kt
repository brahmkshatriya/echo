package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.QuickSearchItem

interface SearchClient {
    suspend fun quickSearch(query: String): List<QuickSearchItem>
    fun search(query: String): PagedData<MediaItemsContainer>
}