package dev.brahmkshatriya.echo.db.models

import androidx.room.Entity

@Entity(primaryKeys = ["id"])
data class DownloadEntity(
    val id: Long,
    val itemId: String,
    val clientId: String,
    val groupName: String? = null,
    val downloadPath: String,
)