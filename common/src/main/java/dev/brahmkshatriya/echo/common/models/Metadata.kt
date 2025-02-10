package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.helpers.ImportType

/**
 * Metadata for an extension.
 *
 * @property className The class name of the extension
 * @property path The file path of the extension's file
 * @property importType The type of import for the extension
 * @property id The id of the extension
 * @property name The name of the extension
 * @property version The version of the extension
 * @property description The description of the extension
 * @property author The author of the extension
 * @property authorUrl The author's site to open
 * @property iconUrl The icon URL of the extension
 * @property repoUrl The repository URL of the extension
 * @property updateUrl The update URL of the extension
 * @property preservedPackages The packages to preserve, ideally for extensions that use native libraries
 * @property enabled Whether the extension is enabled
 */
data class Metadata(
    val className: String,
    val path: String,
    val importType: ImportType,
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val authorUrl: String? = null,
    val iconUrl: String? = null,
    val repoUrl: String? = null,
    val updateUrl: String? = null,
    val preservedPackages: List<String> = listOf(),
    val enabled: Boolean = true
)