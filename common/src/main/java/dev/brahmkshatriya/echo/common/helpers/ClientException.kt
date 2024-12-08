package dev.brahmkshatriya.echo.common.helpers

import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.helpers.ClientException.LoginRequired
import dev.brahmkshatriya.echo.common.helpers.ClientException.NotSupported
import dev.brahmkshatriya.echo.common.helpers.ClientException.Unauthorized

/**
 * A base exception class for handled client exceptions,
 * the extension can throw other exceptions too, but they will be handled by the App.
 *
 * The app handles the following exceptions:
 * - [LoginRequired] - When the user is not logged in
 * - [Unauthorized] - When the user is not authorized, will log out the user.
 * - [NotSupported] - When the extension does not support an operation.
 *
 * @see [LoginClient]
 */
sealed class ClientException : Exception() {
    /**
     * To be thrown when the some operation requires user to be logged in.
     */
    open class LoginRequired : ClientException()

    /**
     * To be thrown when the user is not authorized to perform an operation.
     * The user will be logged out from the app.
     *
     * @param userId The id of the user
     */
    class Unauthorized(val userId: String) : LoginRequired()

    /**
     * To be thrown when the extension does not support an operation.
     *
     * @param operation The name of the operation that is not supported
     */
    class NotSupported(val operation: String) : ClientException()
}