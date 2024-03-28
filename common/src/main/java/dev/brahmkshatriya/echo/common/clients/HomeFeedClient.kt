package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer

interface HomeFeedClient {
    suspend fun getHomeGenres(): List<Genre>
    fun getHomeFeed(genre: Genre?): PagedData<MediaItemsContainer>

}
