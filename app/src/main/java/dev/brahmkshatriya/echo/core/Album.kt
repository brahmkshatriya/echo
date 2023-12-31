package dev.brahmkshatriya.echo.core

import android.net.Uri

data class Album(
    val uri: Uri,
    val title: String,
    val artists: List<Artist>,
    val tracks: List<Track>,
    val cover: FileUrl?,
    val releaseDate: String?,
    val publisher: String?,
    val duration: Long?,
    val description: String?,
    val genres: List<String> = listOf(),
)

data class FileUrl(
    val url: String,
    val headers: Map<String, String> = mapOf()
)

open class User(
    open val uri: Uri,
    open val name: String,
    open val cover: FileUrl?,
)

data class Artist(
    override val uri: Uri,
    override val name: String,
    override val cover: FileUrl?,
    val description: String?,
    val followers: Int? = null,
) : User(uri, name, cover)

data class Track(
    val uri: Uri,
    val title: String,
    val artists: List<Artist>,
    val cover: FileUrl?,
    val duration: Long?,
    val plays: Int,
    val releaseDate: String?,
    val genres: List<String> = listOf(),
    val description: String?,
)

data class Playlist(
    val uri: Uri,
    val title: String,
    val author: User,
    val tracks: List<Track>,
    val cover: FileUrl?,
    val releaseDate: String?,
    val publisher: String?,
    val duration: Long?,
    val description: String?,
    val genres: List<String> = listOf(),
)