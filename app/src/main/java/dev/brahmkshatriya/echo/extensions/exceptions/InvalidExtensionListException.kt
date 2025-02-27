package dev.brahmkshatriya.echo.extensions.exceptions

class InvalidExtensionListException(
    val link: String, override val cause: Throwable
) : Exception()