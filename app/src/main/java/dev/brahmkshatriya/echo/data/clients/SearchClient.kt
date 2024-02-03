package dev.brahmkshatriya.echo.data.clients

import androidx.paging.PagingData
import dev.brahmkshatriya.echo.data.models.MediaItemsContainer
import dev.brahmkshatriya.echo.data.models.QuickSearchItem
import kotlinx.coroutines.flow.Flow

interface SearchClient {
    suspend fun quickSearch(query: String): List<QuickSearchItem>
    suspend fun search(query: String): Flow<PagingData<MediaItemsContainer>>
}