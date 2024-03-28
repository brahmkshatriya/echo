package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.QuickSearchItem

interface SearchClient {
    suspend fun quickSearch(query: String?): List<QuickSearchItem>
    suspend fun searchGenres(): List<Genre>
    fun search(query: String?, genre: Genre?): PagedData<MediaItemsContainer>
}