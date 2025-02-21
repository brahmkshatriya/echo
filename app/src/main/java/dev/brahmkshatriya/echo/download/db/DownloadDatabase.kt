package dev.brahmkshatriya.echo.download.db

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.brahmkshatriya.echo.download.db.models.EchoMediaItemEntity
import dev.brahmkshatriya.echo.download.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.download.db.models.TrackDownloadTaskEntity

@Database(
    entities = [
        MediaTaskEntity::class,
        EchoMediaItemEntity::class,
        TrackDownloadTaskEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao

    companion object {
        private const val DATABASE_NAME = "download-database"
        fun create(app: Application) = Room.databaseBuilder(
            app, DownloadDatabase::class.java, DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }
}