package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Canvas (
    val data: Data? = null,
) {

    @Serializable
    data class Data(
        val trackUnion: TrackUnion? = null
    )

    @Serializable
    data class TrackUnion(
        @SerialName("__typename")
        val typename: String? = null,

        val canvas: CanvasClass? = null,
        val uri: String? = null
    )

    @Serializable
    data class CanvasClass(
        val fileId: String? = null,
        val type: String? = null,
        val uri: String? = null,
        val url: String? = null
    )
}