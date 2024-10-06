package dev.brahmkshatriya.echo

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.brahmkshatriya.echo.db.DownloadDao
import dev.brahmkshatriya.echo.db.ExtensionDao
import dev.brahmkshatriya.echo.db.UserDao
import dev.brahmkshatriya.echo.db.models.CurrentUser
import dev.brahmkshatriya.echo.db.models.DownloadEntity
import dev.brahmkshatriya.echo.db.models.ExtensionEntity
import dev.brahmkshatriya.echo.db.models.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CurrentUser::class,
        DownloadEntity::class,
        ExtensionEntity::class
    ],
    version = 4
)
abstract class EchoDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun downloadDao(): DownloadDao
    abstract fun extensionDao(): ExtensionDao
}