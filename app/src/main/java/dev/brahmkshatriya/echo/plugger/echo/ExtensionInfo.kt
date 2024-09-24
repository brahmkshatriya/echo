package dev.brahmkshatriya.echo.plugger.echo

import dev.brahmkshatriya.echo.common.models.ExtensionType

data class ExtensionInfo(
    val extensionMetadata: ExtensionMetadata,
    val extensionType: ExtensionType,
    val id: String = extensionMetadata.id,
    val name: String = extensionMetadata.name,
) {
    companion object {
        fun ExtensionMetadata.toExtensionInfo(extensionType: ExtensionType) =
            ExtensionInfo(this, extensionType)
    }
}