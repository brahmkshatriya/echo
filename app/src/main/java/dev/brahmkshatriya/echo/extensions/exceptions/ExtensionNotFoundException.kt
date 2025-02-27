package dev.brahmkshatriya.echo.extensions.exceptions

class ExtensionNotFoundException(val id: String?) : Exception("Extension not found: $id")