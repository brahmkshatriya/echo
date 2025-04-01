package dev.brahmkshatriya.echo.common.models

/**
 * Progress data class to hold the progress of the download.
 *
 * @param size The total size of the file, 0 if unknown.
 * @param progress The progress of the download.
 * @param speed The speed of the download in bytes per second, 0 if unknown.
 */
data class Progress(
    val size: Long = 0,
    val progress: Long = 0,
    val speed: Long = 0,
)