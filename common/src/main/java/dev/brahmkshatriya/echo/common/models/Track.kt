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
    val isExplicit: Boolean = false,
    val genre: String? = null,
    val subtitle: String? = null,
    val extras: Map<String, String> = mapOf(),
    val streamables: List<Streamable> = listOf(),
    val isLiked: Boolean = false,
    val isHidden: Boolean = false
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

    private val mediaStreamables: List<Streamable> by lazy {
        streamables.filter { it.mediaType == Streamable.MediaType.AudioVideo }
    }
}