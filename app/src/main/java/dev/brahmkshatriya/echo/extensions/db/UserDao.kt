package dev.brahmkshatriya.echo.extensions.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.extensions.db.models.CurrentUser
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCurrentUser(currentUser: CurrentUser)

    @Query("SELECT * FROM CurrentUser")
    fun observeCurrentUser(): Flow<List<CurrentUser>>

    @Query("SELECT * FROM UserEntity WHERE type = :type AND extId = :extId")
    suspend fun getAllUsers(type: ExtensionType, extId: String): List<UserEntity>

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("SELECT * FROM UserEntity WHERE type = :type AND extId = :extId AND id = :userId")
    suspend fun getUser(type: ExtensionType, extId: String, userId: String?): UserEntity?

}