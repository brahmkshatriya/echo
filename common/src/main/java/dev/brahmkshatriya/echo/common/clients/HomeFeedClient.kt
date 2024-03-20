package dev.brahmkshatriya.echo.common.clients

import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface HomeFeedClient {
    suspend fun getHomeGenres(): List<Genre>
    suspend fun getHomeFeed(genre: StateFlow<Genre?>): Flow<PagingData<MediaItemsContainer>>

}
