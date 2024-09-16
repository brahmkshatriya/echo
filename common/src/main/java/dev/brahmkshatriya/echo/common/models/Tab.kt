package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

@Serializable
data class Tab (
    val id: String,
    val name: String,
    val extras: Map<String, String> = emptyMap(),
)