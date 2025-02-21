package dev.brahmkshatriya.echo.extensions.exceptions

data class ExtensionNotFoundException(val id: String?) : Exception("Extension not found: $id")