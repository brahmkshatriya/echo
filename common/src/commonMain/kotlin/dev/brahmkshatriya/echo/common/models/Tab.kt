package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.models.Feed.Companion.loadAll
import kotlinx.serialization.Serializable

/**
 * A data class representing a tab
 *
 * @property id The id of the tab
 * @property title The title of the tab
 * @property isSort Whether the tab is a sort tab, if true, it will not ne considered for loading in the [Feed.loadAll] method.
 * @property extras Any extra data you want to associate with the tab
 */
@Serializable
data class Tab(
    val id: String,
    val title: String,
    val isSort: Boolean = false,
    val extras: Map<String, String> = emptyMap(),
)