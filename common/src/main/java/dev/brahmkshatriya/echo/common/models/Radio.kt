package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

/**
 * A data class representing a radio station
 *
 * @property id The id of the radio station
 * @property title The title of the radio station
 * @property cover The cover image of the radio station
 * @property tabs The tabs of the radio station (not used yet)
 * @property subtitle The subtitle of the radio station, used to display information under the title
 * @property extras Any extra data you want to associate with the radio station
 */
@Serializable
data class Radio(
    val id: String,
    val title: String,
    val cover: ImageHolder? = null,
    //NOT USED
    val tabs: List<Tab?> = listOf(),
    val subtitle: String? = null,
    val extras: Map<String, String> = mapOf()
)