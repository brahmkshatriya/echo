package dev.brahmkshatriya.echo.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CurrentUser(
    @PrimaryKey
    val clientId: String,
    val id: String?
)