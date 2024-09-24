package dev.brahmkshatriya.echo.plugger.echo

data class RequiredExtensionsException(
    val requiredExtensions: List<String>
) : Exception("Required extensions not found")