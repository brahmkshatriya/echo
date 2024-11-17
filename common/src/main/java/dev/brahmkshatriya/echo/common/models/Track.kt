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
    val plays: Long? = null,
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
    val subtitles: List<Streamable> by lazy {
        streamables.filter { it.type == Streamable.MediaType.Subtitle }
    }

    val sources: List<Streamable> by lazy {
        streamables.filter { it.type == Streamable.MediaType.Source }
    }

    val backgrounds: List<Streamable> by lazy {
        streamables.filter { it.type == Streamable.MediaType.Background }
    }
}