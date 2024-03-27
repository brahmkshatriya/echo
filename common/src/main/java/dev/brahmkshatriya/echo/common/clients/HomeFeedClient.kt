package dev.brahmkshatriya.echo.common.clients

import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import kotlinx.coroutines.flow.Flow

interface HomeFeedClient {
    suspend fun getHomeGenres(): List<Genre>
    suspend fun getHomeFeed(genre: Genre?): Flow<PagingData<MediaItemsContainer>>

}
