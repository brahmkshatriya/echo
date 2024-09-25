package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.helpers.ImportType

data class Metadata(
    val className: String,
    val path: String,
    val importType: ImportType,
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val iconUrl: String?,
    val enabled: Boolean = true
)