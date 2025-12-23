package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchDesktop(
    val data: Data
) {
    @Serializable
    data class Data(
        val searchV2: SearchV2
    )

    @Serializable
    data class SearchV2(
        val chipOrder: ChipOrder? = null,
        val topResultsV2: TopResultsV2? = null,
        val tracksV2: TracksV2? = null,
        val artists: SearchItems? = null,
        val albumsV2: SearchItems? = null,
        val playlists: SearchItems? = null,
        val podcasts: SearchItems? = null,
        val episodes: SearchItems? = null,
        val audiobooks: SearchItems? = null,
        val users: SearchItems? = null,
        val genres: SearchItems? = null
    )

    @Serializable
    data class TracksV2(
        val items: List<TrackWrapperWrapper>? = null,
        val pagingInfo: PagingInfo? = null,
        val totalCount: Long? = null
    )

    @Serializable
    data class SearchItems(
        val items: List<Item.Wrapper>? = null,
        val pagingInfo: PagingInfo? = null,
        val totalCount: Long? = null
    )

    @Serializable
    data class ChipOrder(
        val items: List<ChipOrderItem>? = null
    )

    @Serializable
    data class ChipOrderItem(
        val typeName: String? = null
    )

    @Serializable
    data class TopResultsV2(
        val featured: List<Item.Wrapper>? = null,
        val itemsV2: List<ItemWrapperWrapper>? = null
    )

    @Serializable
    data class ItemWrapperWrapper(
        val item: Item.Wrapper? = null
    )

    @Serializable
    data class TrackWrapperWrapper(
        val item: Item.TrackWrapper? = null
    )
}