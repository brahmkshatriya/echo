package dev.brahmkshatriya.echo.common.data.clients

import androidx.paging.PagingData
import dev.brahmkshatriya.echo.common.data.models.MediaItemsContainer
import kotlinx.coroutines.flow.Flow

interface HomeFeedClient {
    suspend fun getHomeGenres() : List<String>
    suspend fun getHomeFeed(genre:String?) : Flow<PagingData<MediaItemsContainer>>

}