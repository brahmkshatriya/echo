package dev.brahmkshatriya.echo.plugger.echo

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.plugger.echo.ExtensionInfo.Companion.toExtensionInfo

open class GenericExtension(
    val type : ExtensionType,
    open val metadata: ExtensionMetadata,
    open val client: ExtensionClient,
    open val info: ExtensionInfo = metadata.toExtensionInfo(type),
)