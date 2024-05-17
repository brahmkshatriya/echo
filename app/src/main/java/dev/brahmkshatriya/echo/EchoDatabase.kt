package dev.brahmkshatriya.echo

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.brahmkshatriya.echo.dao.DownloadDao
import dev.brahmkshatriya.echo.dao.UserDao
import dev.brahmkshatriya.echo.models.CurrentUser
import dev.brahmkshatriya.echo.models.DownloadEntity
import dev.brahmkshatriya.echo.models.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CurrentUser::class,
        DownloadEntity::class
    ],
    version = 2
)
abstract class EchoDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun downloadDao(): DownloadDao
}