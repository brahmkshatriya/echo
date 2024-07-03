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
    fun setCurrentUser(currentUser: CurrentUser)

    @Query("DELETE FROM CurrentUser WHERE clientId = :clientId")
    fun deleteCurrentUser(clientId: String)


    @Query("SELECT * FROM CurrentUser")
    fun observeCurrentUser() : Flow<List<CurrentUser>>

    @Query("""
        SELECT UserEntity.* FROM UserEntity
        INNER JOIN CurrentUser 
        ON UserEntity.id = CurrentUser.id
        AND UserEntity.clientId = CurrentUser.clientId
        WHERE CurrentUser.clientId = :clientId
    """)
    fun getCurrentUser(clientId: String): UserEntity?

    @Query("SELECT * FROM UserEntity WHERE clientId = :clientId")
    fun getAllUsers(clientId: String): List<UserEntity>

    @Query("SELECT * FROM UserEntity WHERE id = :userId AND clientId = :clientId")
    fun getUser(clientId: String?, userId: String?) : UserEntity?

    @Query("DELETE FROM UserEntity WHERE id = :userId AND clientId = :clientId")
    fun deleteUser(userId: String, clientId: String)

    @Query("DELETE FROM UserEntity WHERE clientId = :clientId")
    fun deleteAllUsers(clientId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setUsers(users: List<UserEntity>)
}