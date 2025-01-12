package dev.brahmkshatriya.echo.utils.ui

import java.util.Locale
import kotlin.math.roundToLong

fun Long.toTimeString(): String {
    val seconds = (this.toFloat() / 1000).roundToLong()
    val minutes = seconds / 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
    } else {
        String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds % 60)
    }
}

fun Long.toTimeAgo() = when (val diff = System.currentTimeMillis() / 1000 - this) {
    in 0..59 -> "Just now"
    in 60..3599 -> "${diff / 60}min ago"
    in 3600..86399 -> "${diff / 3600}h ago"
    in 86400..2591999 -> "${diff / 86400}d ago"
    in 2592000..31535999 -> "${diff / 2592000}m ago"
    else -> "${diff / 31536000}y ago"
}