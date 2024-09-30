package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.User

interface UserClient {
    suspend fun loadUser(user: User): User
    fun getShelves(it: User): PagedData<Shelf>
}