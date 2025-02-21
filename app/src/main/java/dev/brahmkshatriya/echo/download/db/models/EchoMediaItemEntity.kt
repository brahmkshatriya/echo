package dev.brahmkshatriya.echo.download.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.utils.Serializer.toData

@Entity
data class EchoMediaItemEntity(
    @PrimaryKey(true)
    val id: Long,
    val data: String,
) {
    val mediaItem by lazy { data.toData<EchoMediaItem>() }
}