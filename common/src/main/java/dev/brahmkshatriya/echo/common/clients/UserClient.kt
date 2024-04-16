package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.User

interface UserClient {
    suspend fun loadUser(user: User): User
    fun getMediaItems(it: User): PagedData<MediaItemsContainer>
}