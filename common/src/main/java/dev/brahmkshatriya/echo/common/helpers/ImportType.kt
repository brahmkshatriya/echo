package dev.brahmkshatriya.echo.common.helpers

/**
 * Enum class to define the type of import,
 * - [BuiltIn]: Imported from the built-in Repo
 * - [App]: Imported from the installed packages
 * - [File]: Imported from the internal storage
 */
enum class ImportType {
    BuiltIn, App, File,
}
