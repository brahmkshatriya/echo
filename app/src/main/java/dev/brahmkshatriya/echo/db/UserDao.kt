package dev.brahmkshatriya.echo.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.brahmkshatriya.echo.db.models.CurrentUser
import dev.brahmkshatriya.echo.db.models.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCurrentUser(currentUser: CurrentUser)

    @Query("SELECT * FROM CurrentUser")
    fun observeCurrentUser(): Flow<List<CurrentUser>>

    @Query(
        """
        SELECT UserEntity.* FROM UserEntity
        INNER JOIN CurrentUser 
        ON UserEntity.id = CurrentUser.id
        AND UserEntity.clientId = CurrentUser.clientId
        WHERE CurrentUser.clientId = :clientId
    """
    )
    suspend fun getCurrentUser(clientId: String?): UserEntity?

    @Query("SELECT * FROM UserEntity WHERE clientId = :clientId")
    suspend fun getAllUsers(clientId: String): List<UserEntity>

    @Query("DELETE FROM UserEntity WHERE id = :userId AND clientId = :clientId")
    suspend fun deleteUser(userId: String, clientId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setUsers(users: List<UserEntity>)

}