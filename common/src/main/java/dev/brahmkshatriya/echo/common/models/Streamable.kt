package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable
import java.io.Serializable as JSerializable

@Serializable
data class Streamable(
    val id: String,
    val quality: Int,
    val extra: Map<String, String> = mapOf()
) : JSerializable