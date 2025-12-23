package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class HomeSection(
    val data: Data
 ) {

    @Serializable
    data class Data (
        val homeSections: HomeSections
    )

    @Serializable
    data class HomeSections (
        val sections: List<Section>
    )

    @Serializable
    data class Section (

        val data: SectionData? = null,
        val sectionItems: Sections.Items,
        val uri: String? = null
    )

    @Serializable
    data class SectionData (
        val subtitle: Title? = null,
        val title: Title? = null
    )
}