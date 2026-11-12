package com.polysius.applocker.rules

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

const val MINUTES_PER_DAY = 24 * 60

/**
 * A recurring daily window such as "13:00 to 16:00 on weekdays".
 *
 * Times are minutes since local midnight. A window whose end is not after its start crosses
 * midnight, and it belongs to the day it starts: a Monday 21:45 to 05:45 window is still active at
 * 03:00 on Tuesday, and Tuesday's own days setting has no say in that.
 */
data class TimeWindow(
    val days: Set<DayOfWeek>,
    val startMinutes: Int,
    val endMinutes: Int,
) {
    /** The moment the window ends if [now] is inside it, or null if it is not active. */
    fun activeUntil(now: LocalDateTime): LocalDateTime? {
        val minuteOfDay = now.hour * 60 + now.minute
        val today = now.toLocalDate()
        val crossesMidnight = endMinutes <= startMinutes

        if (!crossesMidnight) {
            val inside = today.dayOfWeek in days && minuteOfDay in startMinutes until endMinutes
            return if (inside) today.atMinutes(endMinutes) else null
        }

        val startedToday = today.dayOfWeek in days && minuteOfDay >= startMinutes
        if (startedToday) return today.plusDays(1).atMinutes(endMinutes)

        val startedYesterday = today.minusDays(1).dayOfWeek in days && minuteOfDay < endMinutes
        if (startedYesterday) return today.atMinutes(endMinutes)

        return null
    }

    fun isActive(now: LocalDateTime): Boolean = activeUntil(now) != null
}

private fun LocalDate.atMinutes(minutes: Int): LocalDateTime =
    atTime(LocalTime.of(minutes / 60, minutes % 60))
