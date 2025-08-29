package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale

@Suppress("MemberVisibilityCanBePrivate")
@Serializable
data class Date(
    val epochTimeMs: Long
) : Comparable<Date> {

    val calendar by lazy {
        Calendar.getInstance()!!.apply {
            timeInMillis = epochTimeMs
        }
    }

    val date by lazy { calendar.time!! }

    constructor(
        year: Int,
        month: Int? = null,
        day: Int? = null,
    ) : this(
        Calendar.getInstance().apply {
            set(year, (month ?: 1) - 1, day ?: 1)
        }.timeInMillis
    )

    companion object {
        fun Int.toYearDate() = Date(this)
    }


    val year: Int by lazy { calendar.get(Calendar.YEAR) }
    val month: Int? by lazy {
        val isFirstDayOfYear =
            calendar.get(Calendar.MONTH) == Calendar.JANUARY && calendar.get(Calendar.DAY_OF_MONTH) == 1
        if (!isFirstDayOfYear) calendar.get(Calendar.MONTH) + 1 else null
    }

    val day: Int? by lazy {
        if (calendar.get(Calendar.DAY_OF_MONTH) == 1) null
        else calendar.get(Calendar.DAY_OF_MONTH)
    }

    override fun compareTo(other: Date): Int {
        return date.compareTo(other.date)
    }

    override fun toString(): String = when {
        month == null || day == null -> year.toString()
        else -> DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(date)
    }

}