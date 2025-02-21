package dev.brahmkshatriya.echo.download.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.utils.Serializer.toData

@Entity
data class TrackDownloadTaskEntity(
    @PrimaryKey(true)
    val id: Long,
    val extensionId: String,
    val contextId: Long?,
    val data: String,
    val sortOrder: Int? = null,
    val loaded: Boolean = false,
    val folderPath: String? = null,
    val streamableId: String? = null,
    val indexesData: String? = null,
    val toMergeFiles: String? = null,
    val toTagFile: String? = null,
    val finalFile: String? = null,
) {
    val track by lazy { data.toData<Track>() }
    val indexes by lazy { indexesData?.toData<List<Int>>().orEmpty() }
}