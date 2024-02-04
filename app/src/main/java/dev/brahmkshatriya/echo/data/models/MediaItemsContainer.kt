package dev.brahmkshatriya.echo.data.models

data class MediaItemsContainer(
    val title: String,
    val list: List<MediaItem>,
    val subtitle: String? = null
)