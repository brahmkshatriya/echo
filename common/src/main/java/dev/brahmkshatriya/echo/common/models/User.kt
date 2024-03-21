package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
open class User(
    open val id: String,
    open val name: String,
    open val cover: ImageHolder? = null,
    open val extras: Map<String, String> = mapOf()
) : Parcelable