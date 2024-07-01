package dev.brahmkshatriya.echo.common.exceptions

import dev.brahmkshatriya.echo.common.models.ExtensionType

open class LoginRequiredException(
    open val clientId: String,
    open val clientName: String,
    open val clientType: ExtensionType
) : Exception("Login Required ($clientId : $clientName)")