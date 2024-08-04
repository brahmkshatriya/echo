package dev.brahmkshatriya.echo.plugger

data class RequiredExtensionsException(
    val requiredExtensions: List<String>
) : Exception("Required extensions not found")