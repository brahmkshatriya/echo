package dev.brahmkshatriya.echo.extensions

import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import kotlinx.serialization.Serializable

@Serializable
sealed interface MediaState<T : EchoMediaItem> {
    val extensionId: String
    val item: T
    val loaded: Boolean

    @Serializable
    class Loaded<T : EchoMediaItem>(
        override val extensionId: String,
        override val item: T,
        val isFollowed: Boolean?,
        val followers: Long?,
        val isSaved: Boolean?,
        val isLiked: Boolean?,
        val isHidden: Boolean?,
        val showRadio: Boolean,
        val showShare: Boolean
    ) : MediaState<T> {
        override val loaded = true
    }

    @Serializable
    data class Unloaded<T : EchoMediaItem>(
        override val extensionId: String,
        override val item: T
    ) : MediaState<T> {
        override val loaded = false
    }
}