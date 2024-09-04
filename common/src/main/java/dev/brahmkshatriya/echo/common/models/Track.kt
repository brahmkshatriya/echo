package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artists: List<Artist> = listOf(),
    val album: Album? = null,
    val cover: ImageHolder? = null,
    val duration: Long? = null,
    val plays: Int? = null,
    val releaseDate: String? = null,
    val description: String? = null,
    val liked: Boolean = false,
    val extras: Map<String, String> = mapOf(),
    val streamables: List<Streamable> = listOf(),
    val expiresAt: Long = 0,
) {
    val subtitleStreamables: List<Streamable> by lazy {
        streamables.filter { it.mediaType == Streamable.MediaType.Subtitle }
    }

    val audioStreamables: List<Streamable> by lazy {
        mediaStreamables + streamables.filter { it.mediaType == Streamable.MediaType.Audio }
    }

    val videoStreamables: List<Streamable> by lazy {
        mediaStreamables + streamables.filter { it.mediaType == Streamable.MediaType.Video }
    }

    val mediaStreamables: List<Streamable> by lazy {
        streamables.filter { it.mediaType == Streamable.MediaType.AudioVideo }
    }
}