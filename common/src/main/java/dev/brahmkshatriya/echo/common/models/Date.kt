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
) : Comparable<Date> {
    companion object {
        fun Int.toDate() = Date(this)
    }

    val date: java.util.Date by lazy {
        val calendar = Calendar.getInstance().apply {
            set(year, (month ?: 1) - 1, day ?: 1)
        }
        calendar.time
    }

    override fun compareTo(other: Date): Int {
        return date.compareTo(other.date)
    }

    override fun toString(): String = when {
        month == null || day == null -> year.takeIf { it != 0 }?.toString() ?: ""
        else -> DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(date)
    }

}