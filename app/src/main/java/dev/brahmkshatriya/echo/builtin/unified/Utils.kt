package dev.brahmkshatriya.echo.builtin.unified

import dev.brahmkshatriya.echo.common.models.Date
import java.util.Calendar

object Utils {
    fun getDateNow(): Date {
        val calendar = Calendar.getInstance()
        calendar.time = java.util.Date()
        return Date(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH),
            day = calendar.get(Calendar.DAY_OF_MONTH),
        )
    }
}