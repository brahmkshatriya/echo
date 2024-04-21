package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Artist(
    override val id: String,
    override val name: String,
    override val cover: ImageHolder? = null,
    override val extras: Map<String, String> = mapOf(),
    val subtitle: String? = null,
    val description: String? = null,
    val followers: Int? = null,
    val isFollowing : Boolean = false,
) : User(id, name, cover), Parcelable