package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.TrackerExtension

/**
 * Interface to provide installed [TrackerExtension]s to this extension
 */
interface TrackerExtensionsProvider {
    /**
     * List of required [TrackerExtension]s. If empty, all installed extensions will be provided.
     * If not empty, only the extensions with the specified ids will be provided
     */
    val requiredTrackerExtensions: List<String>

    /**
     * Called when the extension is initialized,
     * to provide the [requiredTrackerExtensions] to the extension.
     *
     * Also called when the extension list is updated.
     */
    fun setTrackerExtensions(extensions: List<TrackerExtension>)
}