package dev.brahmkshatriya.echo.common.models

data class ExtensionMetadata(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val iconUrl: ImageHolder?
)