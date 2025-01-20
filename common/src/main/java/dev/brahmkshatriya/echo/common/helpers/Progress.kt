package dev.brahmkshatriya.echo.common.helpers

import dev.brahmkshatriya.echo.common.helpers.Progress.Downloading
import dev.brahmkshatriya.echo.common.helpers.Progress.Final
import dev.brahmkshatriya.echo.common.helpers.Progress.Initialized
import dev.brahmkshatriya.echo.common.helpers.Progress.Paused

/**
 * A sealed class representing the progress of a download.
 *
 * ```
 *     Initial         Progressing        Final
 * -------------------------------------------------
 * |  Initialized  🡺  Downloading  🡺  Completed  |
 * |                     🡹  🡻     🡺  Failed     |
 * |                     Paused                    |
 * -------------------------------------------------
 * ```
 * @see Initialized
 * @see Downloading
 * @see Paused
 * @see Final.Completed
 * @see Final.Failed
 */
sealed class Progress<T> {
    /**
     * Represents the initial state of a download.
     *
     * @param size The size of the file to be downloaded, `null` if unknown.
     */
    data class Initialized<T>(val size: Long?) : Progress<T>()

    /**
     * Represents the progress of a download.
     *
     * @param downloaded The number of bytes downloaded so far.
     * @param speed The download speed in bytes per second.
     */
    data class Downloading<T>(val downloaded: Long, val speed: Long?) : Progress<T>()

    /**
     * Represents the paused state of a download.
     *
     * @param downloaded The number of bytes downloaded so far.
     */
    data class Paused<T>(val downloaded: Long) : Progress<T>()

    /**
     * Represents the final state of a download.
     */
    sealed class Final<T> : Progress<T>() {
        /**
         * Represents the successful completion of a download.
         *
         * @param finalSize The final size of the downloaded file.
         * @param data The downloaded file.
         */
        data class Completed<T>(val finalSize: Long, val data: T) : Final<T>()

        /**
         * Represents the failure of a download.
         *
         * @param reason The reason for the failure.
         */
        data class Failed<T>(val reason: Throwable) : Final<T>()
    }
}