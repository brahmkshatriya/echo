package dev.brahmkshatriya.echo.extensions

data class ExtensionNotFoundException(val id: String?) : Exception("Extension not found: $id")