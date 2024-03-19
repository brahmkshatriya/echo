package dev.brahmkshatriya.echo.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
open class Streamable(
    val id: String
) : Parcelable