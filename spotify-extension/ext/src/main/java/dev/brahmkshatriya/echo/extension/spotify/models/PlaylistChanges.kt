package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistChanges(
    val revision: String? = null,
    val resultingRevisions: List<String>? = null,
    val multipleHeads: Boolean? = null,
    val capabilities: Capabilities? = null,
) {
    @Serializable
    data class Capabilities(
        val canView: Boolean? = null,
        val canAdministratePermissions: Boolean? = null,
        val grantableLevel: List<String>? = null,
        val canEditMetadata: Boolean? = null,
        val canEditItems: Boolean? = null,
        val canCancelMembership: Boolean? = null,
    )
}