package dev.brahmkshatriya.echo.plugger

import tel.jeelpa.plugger.models.PluginMetadata

data class ExtensionMetadata(
    override val className: String,
    override val path: String,
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val iconUrl: String?
) : PluginMetadata