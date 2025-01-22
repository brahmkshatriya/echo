package dev.brahmkshatriya.echo.common.helpers

import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * Represents the progress of a file to download.
 *
 * @see Progress
 */
typealias FileProgress = Progress<File>

/**
 * Represents a task used to track the progress of a download/merge task.
 * Send the [Progress] of the task to the [progressFlow] state flow.
 *
 * Once a [Progress.Final] state is reached, the task is considered complete.
 *
 * @see Progress
 * @see MutableStateFlow
 */
interface FileTask {
    /**
     * The progress of the task.
     */
    val progressFlow: MutableStateFlow<FileProgress>

    /**
     * What to do when the task is started.
     */
    val start: SuspendedFunction

    /**
     * What to do when the task is cancelled.
     */
    val cancel: SuspendedFunction

    /**
     * Pause the task. Use null if pausing is not supported.
     */
    val pause: SuspendedFunction?

    /**
     * Resume the task. Use null if pausing is not supported.
     */
    val resume: SuspendedFunction?

    /**
     * Whether the task supports pausing.
     */
    fun supportsPause(): Boolean = pause != null && resume != null
}
