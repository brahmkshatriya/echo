package dev.brahmkshatriya.echo.extensions.exceptions

import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.helpers.ClientException

sealed class AppException : Exception() {

    abstract val extension: Extension<*>

    open class LoginRequired(
        override val extension: Extension<*>
    ) : AppException()

    class Unauthorized(
        override val extension: Extension<*>,
        val userId: String
    ) : LoginRequired(extension)

    class NotSupported(
        override val cause: Throwable,
        override val extension: Extension<*>,
        val operation: String
    ) : AppException() {
        override val message: String
            get() = "$operation is not supported in ${extension.name}"
    }

    class Other(
        override val cause: Throwable,
        override val extension: Extension<*>
    ) : AppException() {
        override val message: String
            get() = "${cause.message} error in ${extension.name}"
    }

    companion object {
        fun Throwable.toAppException(extension: Extension<*>): AppException = when (this) {
            is ClientException.Unauthorized -> Unauthorized(extension, userId)
            is ClientException.LoginRequired -> LoginRequired(extension)
            is ClientException.NotSupported -> NotSupported(this, extension, operation)
            else -> Other(this, extension)
        }
    }
}