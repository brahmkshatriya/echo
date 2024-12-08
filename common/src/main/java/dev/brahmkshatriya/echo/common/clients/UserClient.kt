package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.User

/**
 * Used to load a [User] and get its shelves.
 */
interface UserClient {

    /**
     * Loads a user, the unloaded user may only have the id and name.
     *
     * @param user the user to load.
     * @return the loaded user.
     */
    suspend fun loadUser(user: User): User

    /**
     * Gets the shelves of a user. (Like "User Playlists", etc.)
     *
     * @param user the user to get the shelves of.
     * @return the paged shelves.
     *
     * @see PagedData
     * @see Shelf
     */
    fun getShelves(user: User): PagedData<Shelf>
}