package dev.brahmkshatriya.echo.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.brahmkshatriya.echo.db.models.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert
    suspend fun insertDownload(download: DownloadEntity)

    @Query("DELETE FROM DownloadEntity WHERE id = :downloadId")
    suspend fun deleteDownload(downloadId: Long)

    @Query("SELECT * FROM DownloadEntity")
    fun getDownloadsFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM DownloadEntity WHERE id = :downloadId")
    suspend fun getDownload(downloadId: Long): DownloadEntity?
}