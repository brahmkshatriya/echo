package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

@Serializable
data class StreamableVideo(val request: Request, val looping: Boolean, val crop: Boolean)