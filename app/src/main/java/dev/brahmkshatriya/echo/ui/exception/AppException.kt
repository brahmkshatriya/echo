package dev.brahmkshatriya.echo.ui.exception

import dev.brahmkshatriya.echo.common.models.ClientException
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.plugger.ExtensionInfo
import dev.brahmkshatriya.echo.plugger.ExtensionMetadata

sealed class AppException(
    override val cause: Throwable
) : Exception() {

    val extensionId: String
        get() = extensionInfo.id
    val extensionName: String
        get() = extensionInfo.name
    val extensionType: ExtensionType
        get() = extensionInfo.extensionType
    val extensionMetadata: ExtensionMetadata
        get() = extensionInfo.extensionMetadata

    abstract val extensionInfo: ExtensionInfo

    open class LoginRequired(
        override val cause: Throwable,
        override val extensionInfo: ExtensionInfo
    ) : AppException(cause)

    data class Unauthorized(
        override val cause: Throwable,
        override val extensionInfo: ExtensionInfo,
        val userId: String
    ) : LoginRequired(cause, extensionInfo)

    data class NotSupported(
        override val cause: Throwable,
        override val extensionInfo: ExtensionInfo,
        val operation: String
    ) : AppException(cause) {
        override val message: String
            get() = "$operation is not supported in ${extensionInfo.name}"
    }

    data class Other(
        override val cause: Throwable,
        override val extensionInfo: ExtensionInfo
    ) : AppException(cause) {
        override val message: String
            get() = "${cause.message} error in ${extensionInfo.name}"
    }

    companion object {
        fun Throwable.toAppException(extensionInfo: ExtensionInfo): AppException = when (this) {
            is ClientException.Unauthorized -> Unauthorized(this, extensionInfo, userId)
            is ClientException.LoginRequired -> LoginRequired(this, extensionInfo)
            is ClientException.NotSupported -> NotSupported(this, extensionInfo, operation)
            else -> Other(this, extensionInfo)
        }
    }
}