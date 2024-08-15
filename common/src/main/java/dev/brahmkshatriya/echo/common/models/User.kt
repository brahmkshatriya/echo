package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable
import java.io.Serializable as JSerializable

@Serializable
open class User(
    open val id: String,
    open val name: String,
    open val cover: ImageHolder? = null,
    open val extras: Map<String, String> = emptyMap(),
) : JSerializable