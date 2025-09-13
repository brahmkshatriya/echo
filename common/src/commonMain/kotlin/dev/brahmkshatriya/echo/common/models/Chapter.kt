package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.models.Chapter.SkipType.ASK
import dev.brahmkshatriya.echo.common.models.Chapter.SkipType.NONE
import dev.brahmkshatriya.echo.common.models.Chapter.SkipType.SKIP
import kotlinx.serialization.Serializable

/**
 * Represents a chapter in a track.
 * @property name the name of the chapter.
 * @property startTime the start time of the chapter in milliseconds.
 * @property endTime the end time of the chapter in milliseconds, or null if it is not defined.
 * @property skipType the type of skip behavior for the chapter.
 */
@Serializable
data class Chapter(
    val name: String,
    val startTime: Long,
    val endTime: Long? = null,
    val skipType: SkipType = NONE,
) {

    /**
     * Represents the type of skip behavior for a chapter.
     * - [NONE] The chapter is not skipped.
     * - [SKIP] The chapter is automatically skipped.
     * - [ASK] The user is prompted whether to skip the chapter.
     */
    enum class SkipType {
        NONE, SKIP, ASK
    }
}