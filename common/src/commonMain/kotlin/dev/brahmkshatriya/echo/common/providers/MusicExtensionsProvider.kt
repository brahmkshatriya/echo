package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.MusicExtension

/**
 * Interface to provide installed [MusicExtension]s to this extension
 */
interface MusicExtensionsProvider {
    /**
     * List of required [MusicExtension]s. If empty, all installed extensions will be provided.
     * If not empty, only the extensions with the specified ids will be provided
     */
    val requiredMusicExtensions: List<String>

    /**
     * Called when the extension is initialized,
     * to provide the [requiredMusicExtensions] to the extension.
     *
     * Also called when the extension list is updated.
     */
    fun setMusicExtensions(extensions: List<MusicExtension>)
}