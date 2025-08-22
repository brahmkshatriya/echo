package dev.brahmkshatriya.echo.common.models

/**
 * Global settings.
 *
 * @property streamQuality The default quality of the stream
 * @property meteredStreamQuality The quality of the stream on unmetered network
 */
data class GlobalSettings (
    val streamQuality: StreamQuality,
    val meteredStreamQuality: StreamQuality? = null
)