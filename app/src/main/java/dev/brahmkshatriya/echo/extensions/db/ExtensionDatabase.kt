package dev.brahmkshatriya.echo.extensions.db

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.extensions.db.models.CurrentUser
import dev.brahmkshatriya.echo.extensions.db.models.ExtensionEntity
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CurrentUser::class,
        ExtensionEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class ExtensionDatabase : RoomDatabase() {
    private val userDao by lazy { userDao() }
    val currentUsersFlow by lazy { userDao.observeCurrentUser() }
    private val extensionDao by lazy { extensionDao() }
    val extensionEnabledFlow by lazy { extensionDao.getExtensionFlow() }

    abstract fun userDao(): UserDao
    abstract fun extensionDao(): ExtensionDao

    suspend fun getUser(current: CurrentUser): User? {
        return userDao.getUser(current.type, current.extId, current.userId)?.user
    }

    companion object {
        private const val DATABASE_NAME = "extension-database"
        fun create(app: Application) = Room.databaseBuilder(
            app, ExtensionDatabase::class.java, DATABASE_NAME
        ).fallbackToDestructiveMigration(true).build()
    }
}