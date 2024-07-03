package dev.brahmkshatriya.echo.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.db.models.ExtensionEntity

@Dao
interface ExtensionDao {

    @Query("SELECT * FROM ExtensionEntity WHERE type = :type AND id = :id")
    fun getExtension(type: ExtensionType, id: String) : ExtensionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setExtension(extensionEntity: ExtensionEntity)
}