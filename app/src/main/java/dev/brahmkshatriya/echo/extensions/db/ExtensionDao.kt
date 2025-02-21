package dev.brahmkshatriya.echo.extensions.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.brahmkshatriya.echo.extensions.db.models.ExtensionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtensionDao {

    @Query("SELECT * FROM ExtensionEntity")
    fun getExtensionFlow(): Flow<List<ExtensionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setExtension(extensionEntity: ExtensionEntity)
}