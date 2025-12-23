package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class FetchLibraryTracks(
    val data: Data
){
    @Serializable
    data class Data(
        val me: Me
    )

    @Serializable
    data class Me(
        val library: Library
    )

    @Serializable
    data class Library(
        val tracks: Tracks
    )

    @Serializable
    data class Tracks(
        val items: List<Items>,
        val pagingInfo: PagingInfo? = null,
        val totalCount: Long? = null
    )

    @Serializable
    data class Items(
        val track: Item.TrackWrapper,
        val addedAt: Date? = null
    )
}