package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EditablePlaylists(
    val data: Data
) {

    @Serializable
    data class Data(
        val me: Me
    )

    @Serializable
    data class Me(
        val editablePlaylists: EditablePlaylists? = null
    )

    @Serializable
    data class EditablePlaylists(
        @SerialName("__typename")
        val typename: String? = null,

        val items: List<ItemElement>,
        val pagingInfo: PagingInfo? = null,
        val totalCount: Long? = null
    )

    @Serializable
    data class ItemElement(
        val curates: Boolean? = null,
        val item: Item.Wrapper,
        val pinned: Boolean? = null
    )
}