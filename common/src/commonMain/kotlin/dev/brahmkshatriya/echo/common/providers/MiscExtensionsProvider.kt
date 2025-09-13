package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.MiscExtension

/**
 * Interface to provide installed [MiscExtension]s to this extension
 */
interface MiscExtensionsProvider {

    /**
     * List of required [MiscExtension]s. If empty, all installed extensions will be provided.
     * If not empty, only the extensions with the specified ids will be provided
     */
    val requiredMiscExtensions: List<String>

    /**
     * Called when the extension is initialized,
     * to provide the [requiredMiscExtensions] to the extension.
     *
     * Also called when the extension list is updated.
     */
    fun setMiscExtensions(extensions: List<MiscExtension>)
}