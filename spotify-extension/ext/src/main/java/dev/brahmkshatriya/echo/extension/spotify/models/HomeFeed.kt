package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomeFeed(
    val data: Data? = null,
    val extensions: Extensions? = null
) {


    @Serializable
    data class Data(
        val home: Home? = null
    )

    @Serializable
    data class Home(
        @SerialName("__typename")
        val typename: String? = null,
        val homeChips: List<Chip>? = null,
        val sectionContainer: Sections.Container? = null,
        val greeting: Title? = null
    )

    @Serializable
    data class Chip(
        val id: String? = null,
        val label: Title? = null
    )

    @Serializable
    data class Extensions(
        val responseIds: ResponseIds? = null
    )

    @Serializable
    data class ResponseIds(
        @SerialName("/home")
        val home: HomeClass? = null
    )

    @Serializable
    data class HomeClass(
        val continuum: String? = null
    )
}