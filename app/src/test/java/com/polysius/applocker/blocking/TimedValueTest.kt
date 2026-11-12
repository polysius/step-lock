package com.polysius.applocker.blocking

import org.junit.Assert.assertEquals
import org.junit.Test

class TimedValueTest {

    private var clock = 0L
    private var computeCount = 0

    private fun timedValue(ttl: Long = 60_000L) = TimedValue(ttl, { clock }) {
        computeCount += 1
        setOf("pkg-$computeCount")
    }

    @Test
    fun `first call computes even when the clock reads zero`() {
        clock = 0L
        val value = timedValue()
        assertEquals(setOf("pkg-1"), value.get())
        assertEquals(1, computeCount)
    }

    @Test
    fun `first call computes when the clock reads a large uptime`() {
        clock = 3_600_000L
        val value = timedValue()
        assertEquals(setOf("pkg-1"), value.get())
    }

    @Test
    fun `value is reused inside the ttl`() {
        val value = timedValue()
        value.get()
        clock = 59_999L
        value.get()
        assertEquals(1, computeCount)
    }

    @Test
    fun `value is recomputed once the ttl has passed`() {
        val value = timedValue()
        value.get()
        clock = 60_000L
        assertEquals(setOf("pkg-2"), value.get())
        assertEquals(2, computeCount)
    }
}
