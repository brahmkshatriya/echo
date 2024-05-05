package dev.brahmkshatriya.echo.common.models

data class Tab (
    val id: String,
    val name: String,
    val extras: Map<String, String> = emptyMap(),
)