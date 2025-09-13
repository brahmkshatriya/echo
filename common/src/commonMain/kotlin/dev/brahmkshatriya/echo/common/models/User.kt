package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

/**
 * A data class representing a user
 * 
 * @property id The id of the user
 * @property name The name of the user
 * @property cover The cover image of the user
 * @property subtitle The subtitle of the user, used to display information under the name
 * @property extras Any extra data you want to associate with the user
 */
@Serializable
data class User(
    val id: String,
    val name: String,
    val cover: ImageHolder? = null,
    val subtitle: String? = null,
    val extras: Map<String, String> = emptyMap(),
)