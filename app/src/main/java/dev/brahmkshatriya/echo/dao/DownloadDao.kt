package dev.brahmkshatriya.echo.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.brahmkshatriya.echo.models.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert
    fun insertDownload(download: DownloadEntity)

    @Query("DELETE FROM DownloadEntity WHERE id = :downloadId")
    fun deleteDownload(downloadId: Long)

    @Query("SELECT * FROM DownloadEntity")
    fun getDownloadsFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM DownloadEntity WHERE id = :downloadId")
    fun getDownload(downloadId: Long): DownloadEntity?
}