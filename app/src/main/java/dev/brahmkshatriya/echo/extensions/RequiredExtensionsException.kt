package dev.brahmkshatriya.echo.extensions

data class RequiredExtensionsException(
    val name: String,
    val requiredExtensions: List<String>
) : Exception("Required extensions not found")