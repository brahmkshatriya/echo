package dev.brahmkshatriya.echo.extensions

import dev.brahmkshatriya.echo.common.helpers.ExtensionType

class ExtensionException(
    val type: ExtensionType,
    override val cause: Throwable
) : Exception("Failed to load extension of type: $type")