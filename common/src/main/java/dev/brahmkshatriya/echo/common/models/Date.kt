package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale


@Serializable
data class Date(
    val year: Int,
    val month: Int? = null,
    val day: Int? = null,
) {
    companion object {
        fun Int.toDate() = Date(this)
    }

    private val time by lazy {
        val calendar = Calendar.getInstance().apply {
            set(year, (month ?: 1) - 1, day ?: 1)
        }
        calendar.time
    }

    override fun toString(): String = when {
        month != null || day != null -> year.toString()
        else -> DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(time)
    }

    operator fun compareTo(other: Date): Int {
        return time.compareTo(other.time)
    }
}