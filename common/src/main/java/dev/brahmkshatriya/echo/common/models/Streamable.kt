package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Streamable(
    val id: String,
    val quality: Int,
    val extra: Map<String, String> = mapOf()
) : Parcelable