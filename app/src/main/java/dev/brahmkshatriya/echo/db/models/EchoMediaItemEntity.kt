package dev.brahmkshatriya.echo.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.utils.toData

@Entity
data class EchoMediaItemEntity(
    @PrimaryKey(true)
    val id: Long,
    val data: String,
) {
    val mediaItem by lazy { data.toData<EchoMediaItem>() }
}