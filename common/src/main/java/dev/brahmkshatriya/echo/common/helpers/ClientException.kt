package dev.brahmkshatriya.echo.common.helpers

sealed class ClientException : Exception() {
    open class LoginRequired : ClientException()
    class Unauthorized(val userId: String) : LoginRequired()
    class NotSupported(val operation: String) : ClientException()
}