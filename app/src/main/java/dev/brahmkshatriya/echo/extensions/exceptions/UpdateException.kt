package dev.brahmkshatriya.echo.extensions.exceptions

class UpdateException(override val cause: Throwable) : Exception(cause) {
    override val message: String
        get() = "Update failed: ${cause.message}"
}