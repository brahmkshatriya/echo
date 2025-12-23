package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement

@Serializable
data class Browse (
    val data: Data,
) {

    @Serializable
    data class Data(
        val browse: BrowseClass
    )

    @Serializable
    data class BrowseClass(
        @SerialName("__typename")
        val typename: String? = null,

        val header: Header? = null,
        val sections: Sections,
        val uri: String? = null
    )

    @Serializable
    data class Header(
        val backgroundImage: JsonElement? = null,
        val color: Color? = null,
        val subtitle: JsonElement? = null,
        val title: Title? = null
    )
}