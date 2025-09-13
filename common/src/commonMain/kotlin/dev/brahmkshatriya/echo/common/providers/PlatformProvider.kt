package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.platform.Platform

/**
 * Interface to provide global [Platform] to the extension
 */
interface PlatformProvider {
    /**
     * Called when the extension is initialized, to provide the [Platform] to the extension
     */
    fun setPlatform(platform: Platform)
}
