package dev.brahmkshatriya.echo.extensions.db.models

import androidx.room.Entity
import dev.brahmkshatriya.echo.common.helpers.ExtensionType

@Entity(primaryKeys = ["id", "type"])
data class ExtensionEntity(
    val id: String,
    val type : ExtensionType,
    val enabled : Boolean
)