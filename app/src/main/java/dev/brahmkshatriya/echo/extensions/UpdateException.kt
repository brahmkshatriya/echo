package dev.brahmkshatriya.echo.extensions

data class UpdateException(override val cause: Throwable) : Exception(cause)