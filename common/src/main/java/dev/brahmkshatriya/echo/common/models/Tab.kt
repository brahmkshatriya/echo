package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

/**
 * A data class representing a tab
 *
 * @property id The id of the tab
 * @property title The title of the tab
 * @property extras Any extra data you want to associate with the tab
 */
@Serializable
data class Tab (
    val id: String,
    val title: String,
    val extras: Map<String, String> = emptyMap(),
)