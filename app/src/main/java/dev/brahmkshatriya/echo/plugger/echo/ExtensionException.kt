package dev.brahmkshatriya.echo.plugger.echo

import dev.brahmkshatriya.echo.common.models.ExtensionType

class ExtensionException(
    val type: ExtensionType,
    override val cause: Throwable
) : Exception("Failed to load extension of type: $type")