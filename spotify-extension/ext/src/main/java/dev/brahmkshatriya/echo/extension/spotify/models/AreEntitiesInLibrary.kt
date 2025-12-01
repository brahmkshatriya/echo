package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AreEntitiesInLibrary (
    val data: Data? = null
) {

    @Serializable
    data class Data(
        val lookup: List<Lookup>? = null
    )

    @Serializable
    data class Lookup(
        @SerialName("__typename")
        val typename: String? = null,
        val data: LookupData? = null
    )

    @Serializable
    data class LookupData(
        @SerialName("__typename")
        val typename: String? = null,
        val saved: Boolean? = null
    )
}