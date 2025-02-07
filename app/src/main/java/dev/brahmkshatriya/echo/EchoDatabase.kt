package dev.brahmkshatriya.echo

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.brahmkshatriya.echo.db.DownloadDao
import dev.brahmkshatriya.echo.db.ExtensionDao
import dev.brahmkshatriya.echo.db.UserDao
import dev.brahmkshatriya.echo.db.models.CurrentUser
import dev.brahmkshatriya.echo.db.models.EchoMediaItemEntity
import dev.brahmkshatriya.echo.db.models.ExtensionEntity
import dev.brahmkshatriya.echo.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.db.models.TrackDownloadTaskEntity
import dev.brahmkshatriya.echo.db.models.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CurrentUser::class,
        MediaTaskEntity::class,
        ExtensionEntity::class,
        EchoMediaItemEntity::class,
        TrackDownloadTaskEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class EchoDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun downloadDao(): DownloadDao
    abstract fun extensionDao(): ExtensionDao
}