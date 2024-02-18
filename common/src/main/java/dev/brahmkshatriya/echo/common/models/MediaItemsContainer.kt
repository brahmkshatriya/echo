package dev.brahmkshatriya.echo.common.models

sealed class MediaItemsContainer {
    data class Category(
        val title: String,
        val list: List<EchoMediaItem>,
        val subtitle: String? = null
    ) : MediaItemsContainer()

    data class TrackItem(
        val track: Track,
    ) : MediaItemsContainer()
}