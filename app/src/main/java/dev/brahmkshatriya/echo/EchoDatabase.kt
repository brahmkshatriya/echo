package dev.brahmkshatriya.echo

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.brahmkshatriya.echo.dao.UserDao
import dev.brahmkshatriya.echo.models.CurrentUser
import dev.brahmkshatriya.echo.models.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CurrentUser::class
    ],
    version = 1
)
abstract class EchoDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}