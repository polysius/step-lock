package com.polysius.applocker.rules

import java.time.DayOfWeek
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeWindowTest {

    private val weekdays = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
    )

    // 2026-11-09 is a Monday.
    private fun monday(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, 9, hour, minute)
    private fun tuesday(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, 10, hour, minute)
    private fun saturday(hour: Int) = LocalDateTime.of(2026, 11, 14, hour, 0)
    private fun sunday(hour: Int) = LocalDateTime.of(2026, 11, 15, hour, 0)

    private val afternoon = TimeWindow(weekdays, startMinutes = 13 * 60, endMinutes = 16 * 60)

    @Test
    fun `same day window is active inside and ends at its end time`() {
        assertEquals(monday(16), afternoon.activeUntil(monday(14)))
    }

    @Test
    fun `start is inclusive and end is exclusive`() {
        assertTrue(afternoon.isActive(monday(13)))
        assertFalse(afternoon.isActive(monday(16)))
        assertTrue(afternoon.isActive(monday(15, 59)))
        assertFalse(afternoon.isActive(monday(12, 59)))
    }

    @Test
    fun `window does not apply on days outside its day set`() {
        assertNull(afternoon.activeUntil(saturday(14)))
    }

    private val overnight = TimeWindow(weekdays, startMinutes = 21 * 60 + 45, endMinutes = 5 * 60 + 45)

    @Test
    fun `midnight crossing window is active before midnight and ends next morning`() {
        assertEquals(tuesday(5, 45), overnight.activeUntil(monday(22)))
    }

    @Test
    fun `midnight crossing window is still active after midnight on the day after it started`() {
        assertEquals(tuesday(5, 45), overnight.activeUntil(tuesday(3)))
    }

    @Test
    fun `midnight crossing window belongs to its start day`() {
        // Friday night runs into Saturday morning even though Saturday is not in the day set.
        val fridayNight = LocalDateTime.of(2026, 11, 13, 23, 0)
        assertEquals(saturday(5).plusMinutes(45), overnight.activeUntil(fridayNight))
        assertEquals(saturday(5).plusMinutes(45), overnight.activeUntil(saturday(3)))
        // Saturday night does not start a window, so Sunday 03:00 is clear.
        assertNull(overnight.activeUntil(sunday(3)))
    }

    @Test
    fun `midnight crossing window is inactive between its end and its start`() {
        assertNull(overnight.activeUntil(monday(12)))
        assertNull(overnight.activeUntil(tuesday(5, 45)))
        assertNull(overnight.activeUntil(monday(21, 44)))
    }

    @Test
    fun `window ending at midnight does not cross it`() {
        val untilMidnight = TimeWindow(weekdays, startMinutes = 20 * 60, endMinutes = 0)
        // End 00:00 is not after the start, so it is a midnight crossing window ending at 00:00.
        assertEquals(tuesday(0), untilMidnight.activeUntil(monday(23)))
    }
}
