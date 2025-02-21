package dev.brahmkshatriya.echo.extensions.db.models

import androidx.room.Entity
import dev.brahmkshatriya.echo.common.helpers.ExtensionType

@Entity(primaryKeys = ["type", "extId"])
data class CurrentUser(
    val type : ExtensionType,
    val extId: String,
    val userId: String?
)