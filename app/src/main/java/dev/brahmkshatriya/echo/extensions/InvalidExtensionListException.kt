package dev.brahmkshatriya.echo.extensions

class InvalidExtensionListException(override val cause: Throwable) : Exception(cause)
