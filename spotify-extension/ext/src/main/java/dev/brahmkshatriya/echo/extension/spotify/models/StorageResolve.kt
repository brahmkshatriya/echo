package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StorageResolve (
    val result: String? = null,
    @SerialName("cdnurl")
    val cdnUrl: List<String>,
    @SerialName("fileid")
    val fileId: String? = null,
    val ttl: Long? = null
)