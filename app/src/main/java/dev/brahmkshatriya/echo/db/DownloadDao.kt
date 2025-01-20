package dev.brahmkshatriya.echo.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import dev.brahmkshatriya.echo.db.models.EchoMediaItemEntity
import dev.brahmkshatriya.echo.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.db.models.TrackDownloadTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = REPLACE)
    suspend fun insertDownload(download: MediaTaskEntity) : Long

    @Query("DELETE FROM MediaTaskEntity WHERE id = :downloadId")
    suspend fun deleteDownload(downloadId: Long)

    @Query("SELECT * FROM MediaTaskEntity")
    fun getCurrentDownloadsFlow(): Flow<List<MediaTaskEntity>>

    @Query("SELECT * FROM MediaTaskEntity")
    suspend fun getAllDownloadEntities(): List<MediaTaskEntity>

    @Insert(onConflict = REPLACE)
    suspend fun insertTrackEntity(track: TrackDownloadTaskEntity): Long

    @Query("SELECT * FROM TrackDownloadTaskEntity")
    suspend fun getTracks(): List<TrackDownloadTaskEntity>

    @Query("SELECT * FROM TrackDownloadTaskEntity WHERE contextId = :contextId")
    suspend fun getAllTracksForContext(contextId: Long): List<TrackDownloadTaskEntity>

    @Query("SELECT * FROM TrackDownloadTaskEntity")
    fun getTrackFlow() : Flow<List<TrackDownloadTaskEntity>>

    @Query
    ("SELECT * FROM TrackDownloadTaskEntity WHERE id = :trackId")
    suspend fun getTrackEntity(trackId: Long): TrackDownloadTaskEntity

    @Delete
    suspend fun deleteTrackEntity(track: TrackDownloadTaskEntity)

    @Insert(onConflict = REPLACE)
    suspend fun insertMediaItemEntity(mediaItem: EchoMediaItemEntity) : Long

    @Query("SELECT * FROM EchoMediaItemEntity WHERE id = :id")
    suspend fun getMediaItemEntity(id: Long): EchoMediaItemEntity

    @Delete
    suspend fun deleteMediaItemEntity(mediaItem: EchoMediaItemEntity)

}