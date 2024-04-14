package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.parcelableCreator

@Parcelize
data class Track(
    val id: String,
    val title: String,
    val artists: List<Artist> = listOf(),
    val album: Album? = null,
    val cover: ImageHolder? = null,
    val duration: Long? = null,
    val plays: Int? = null,
    val releaseDate: String? = null,
    val liked: Boolean = false,
    val extras: Map<String, String> = mapOf(),
    val videoStreamable: List<Streamable> = listOf(),
    val audioStreamables: List<Streamable> = listOf(),
    val expiresAt: Long = 0,
) : Parcelable {

    companion object {
        val creator = parcelableCreator<Track>()
    }
}