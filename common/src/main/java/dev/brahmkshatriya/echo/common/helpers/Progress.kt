package dev.brahmkshatriya.echo.common.helpers

import dev.brahmkshatriya.echo.common.helpers.Progress.Final
import dev.brahmkshatriya.echo.common.helpers.Progress.InProgress
import dev.brahmkshatriya.echo.common.helpers.Progress.Initialized
import dev.brahmkshatriya.echo.common.helpers.Progress.Paused

/**
 * A sealed class representing the progress of a download.
 *
 * ```
 *     Initial         Progressing        Final
 * -------------------------------------------------
 * |  Initialized  🡺   InProgress  🡺  Completed  |
 * |                     🡹  🡻     🡺  Cancelled  |
 * |                     Paused     🡺  Failed     |
 * -------------------------------------------------
 * ```
 * @see Initialized
 * @see InProgress
 * @see Paused
 * @see Final.Completed
 * @see Final.Cancelled
 * @see Final.Failed
 */
sealed class Progress<T> {
    /**
     * Represents the initial state of the task.
     *
     * @param size The size of the final data when task is completed, `null` if unknown.
     */
    data class Initialized<T>(val size: Long?) : Progress<T>()

    /**
     * Represents the current progress of the task.
     *
     * @param downloaded The number of bytes progressed so far.
     * @param speed The progress speed in bytes per second.
     */
    data class InProgress<T>(val downloaded: Long, val speed: Long?) : Progress<T>()

    /**
     * Represents the paused state of the task.
     *
     * @param downloaded The number of bytes progressed so far.
     */
    data class Paused<T>(val downloaded: Long) : Progress<T>()

    /**
     * Represents the final state of the task.
     */
    sealed class Final<T> : Progress<T>() {
        /**
         * Represents the successful completion of the task.
         *
         * @param finalSize The final size of the task.
         * @param data The task data.
         */
        data class Completed<T>(val finalSize: Long, val data: T) : Final<T>()


        /**
         * Represents the cancellation of the task.
         */
        class Cancelled<T> : Final<T>()


        /**
         * Represents the failure of the task.
         *
         * @param reason The reason for the failure.
         */
        data class Failed<T>(val reason: Throwable) : Final<T>()
    }
}