package dev.brahmkshatriya.echo.download.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.ui.common.ExceptionUtils
import dev.brahmkshatriya.echo.utils.Serializer.toData
import kotlinx.serialization.Serializable
import java.io.File

@Entity
@Serializable
data class DownloadEntity(
    @PrimaryKey(true)
    val id: Long,
    val extensionId: String,
    val trackId: String,
    val contextId: Long?,
    val sortOrder: Int? = null,
    val data: String,
    val task: TaskType,
    val loaded: Boolean = false,
    val folderPath: String? = null,
    val streamableId: String? = null,
    val indexesData: String? = null,
    val toMergeFilesData: String? = null,
    val toTagFile: String? = null,
    val finalFile: String? = null,
    val exceptionFile: String? = null,
    val fullyDownloaded: Boolean = false
) {
    val track by lazy { data.toData<Track>() }
    val indexes by lazy { indexesData?.toData<List<Int>>().orEmpty() }
    val toMergeFiles by lazy { toMergeFilesData?.toData<List<String>>().orEmpty() }
    val exception by lazy {
        runCatching {
            exceptionFile?.let { File(it) }?.readText()?.toData<ExceptionUtils.Data>()
        }.getOrNull()
    }
    val isFinal by lazy { finalFile != null || exceptionFile != null }
}