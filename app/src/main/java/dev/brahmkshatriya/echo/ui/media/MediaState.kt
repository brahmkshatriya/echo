package dev.brahmkshatriya.echo.ui.media

import dev.brahmkshatriya.echo.common.models.EchoMediaItem

data class MediaState(
    val item: EchoMediaItem,
    val extensionId: String,
    val isFollowed: Boolean?,
    val followers: Long?,
    val isSaved: Boolean?,
    val isLiked: Boolean?,
    val showRadio: Boolean,
    val showShare: Boolean
)
