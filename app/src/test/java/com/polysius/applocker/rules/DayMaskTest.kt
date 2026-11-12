package com.polysius.applocker.rules

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class DayMaskTest {

    @Test
    fun `mask round trips every combination of days`() {
        for (mask in 0..127) {
            assertEquals(mask, mask.toDays().toDayMask())
        }
    }

    @Test
    fun `monday is the lowest bit and sunday the highest`() {
        assertEquals(1, setOf(DayOfWeek.MONDAY).toDayMask())
        assertEquals(64, setOf(DayOfWeek.SUNDAY).toDayMask())
        assertEquals(127, DayOfWeek.entries.toSet().toDayMask())
    }

    @Test
    fun `empty set has an empty mask`() {
        assertEquals(0, emptySet<DayOfWeek>().toDayMask())
    }
}
