package dev.brahmkshatriya.echo.download.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import dev.brahmkshatriya.echo.download.db.models.ContextEntity
import dev.brahmkshatriya.echo.download.db.models.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = REPLACE)
    suspend fun insertContextEntity(context: ContextEntity): Long

    @Insert(onConflict = REPLACE)
    suspend fun insertDownloadEntity(download: DownloadEntity): Long

    @Query("SELECT * FROM DownloadEntity WHERE id = :trackId")
    suspend fun getDownloadEntity(trackId: Long): DownloadEntity?

    @Query("SELECT * FROM ContextEntity WHERE id = :contextId")
    suspend fun getContextEntity(contextId: Long?): ContextEntity?

    @Query("SELECT * FROM DownloadEntity")
    fun getDownloadsFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM ContextEntity")
    fun getContextFlow(): Flow<List<ContextEntity>>

    @Delete
    fun deleteDownloadEntity(download: DownloadEntity)

    @Delete
    fun deleteContextEntity(context: ContextEntity)

    @Query("SELECT * FROM DownloadEntity WHERE contextId = :id")
    fun getDownloadsForContext(id: Long?): List<DownloadEntity>
}