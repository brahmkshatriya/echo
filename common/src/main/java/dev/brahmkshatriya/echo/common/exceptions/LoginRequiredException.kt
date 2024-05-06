package dev.brahmkshatriya.echo.common.exceptions


data class LoginRequiredException(
    val clientId: String,
    val clientName: String
) : Exception("Login Required ($clientId : $clientName)")