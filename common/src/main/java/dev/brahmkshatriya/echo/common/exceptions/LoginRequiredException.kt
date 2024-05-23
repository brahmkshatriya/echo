package dev.brahmkshatriya.echo.common.exceptions

import dev.brahmkshatriya.echo.common.models.ExtensionType

data class LoginRequiredException(
    val clientId: String,
    val clientName: String,
    val clientType: ExtensionType
) : Exception("Login Required ($clientId : $clientName)")