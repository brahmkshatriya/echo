package dev.brahmkshatriya.echo.download.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MediaTaskEntity(
    @PrimaryKey
    val id: Long,
    val trackId: Long,
    val type: TaskType,
    val title: String? = null,
    val supportsPause: Boolean = false,
    val status: Status = Status.Initialized,
    val size: Long? = null,
    val progress: Long = 0,
    val speed: Long? = null,
)