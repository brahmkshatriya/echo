package dev.brahmkshatriya.echo.extensions.exceptions

class RequiredExtensionsMissingException(required: List<String>) : Exception(
    "Missing required extensions: ${required.joinToString(", ")}"
)