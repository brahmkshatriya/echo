package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.LyricsExtension

/**
 * Interface to provide installed [LyricsExtension]s to this extension
 */
interface LyricsExtensionsProvider {
    /**
     * List of required [LyricsExtension]s. If empty, all installed extensions will be provided.
     * If not empty, only the extensions with the specified ids will be provided
     */
    val requiredLyricsExtensions: List<String>

    /**
     * Called when the extension is initialized,
     * to provide the [requiredLyricsExtensions] to the extension.
     *
     * Also called when the extension list is updated.
     */
    fun setLyricsExtensions(extensions: List<LyricsExtension>)
}