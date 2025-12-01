package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class RecentlyPlayed (
    val playContexts: List<PlayContext>
) {

    @Serializable
    data class PlayContext(
        val uri: String,
        val lastPlayedTime: Long? = null,
        val lastPlayedTrackUri: String? = null
    )
}