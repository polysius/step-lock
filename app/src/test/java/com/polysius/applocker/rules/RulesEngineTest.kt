package com.polysius.applocker.rules

import java.time.DayOfWeek
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesEngineTest {

    private val everyDay = DayOfWeek.entries.toSet()

    private fun block(name: String, from: Int, to: Int, enabled: Boolean = true, id: Long = 1) =
        ScheduleRule(id, name, ScheduleKind.BLOCK, TimeWindow(everyDay, from * 60, to * 60), enabled)

    private fun breakWindow(from: Int, to: Int, enabled: Boolean = true, id: Long = 2) =
        ScheduleRule(id, "break", ScheduleKind.BREAK, TimeWindow(everyDay, from * 60, to * 60), enabled)

    // Bedtime 22:30 with a 45 minute lead gives a lock from 21:45 to 05:45.
    private val bedtime = BedtimeRule(22 * 60 + 30, 45, 5 * 60 + 45, everyDay)

    private fun config(vararg rules: ScheduleRule, bedtime: BedtimeRule = this.bedtime) =
        RulesConfig(rules.toList(), bedtime)

    private val lockedApp = "com.example.locked"
    private val scope = AppScope(
        restricted = { it == lockedApp || it == "com.example.allowed" },
        alwaysAllowed = { it == "com.example.allowed" },
    )

    private fun at(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, 11, hour, minute)

    private fun decide(now: LocalDateTime, config: RulesConfig, pkg: String = lockedApp) =
        RulesEngine.decide(pkg, now, config, scope)

    @Test
    fun `app outside the locked set is allowed even during bedtime`() {
        assertEquals(Decision.Allow, decide(at(23), config(), pkg = "com.example.other"))
    }

    @Test
    fun `always allowed app is allowed during bedtime and blocks`() {
        val cfg = config(block("work", 6, 9))
        assertEquals(Decision.Allow, decide(at(7), cfg, pkg = "com.example.allowed"))
        assertEquals(Decision.Allow, decide(at(23), cfg, pkg = "com.example.allowed"))
    }

    @Test
    fun `locked app is allowed when nothing is active`() {
        assertEquals(Decision.Allow, decide(at(12), config(block("work", 6, 9))))
    }

    @Test
    fun `block locks with its name and end time`() {
        val decision = decide(at(7), config(block("work", 6, 9)))
        assertEquals(Decision.Lock(LockReason.SCHEDULE, "work", at(9)), decision)
    }

    @Test
    fun `disabled block does not lock`() {
        assertEquals(Decision.Allow, decide(at(7), config(block("work", 6, 9, enabled = false))))
    }

    @Test
    fun `break overrides a surrounding block`() {
        val cfg = config(block("day", 6, 20), breakWindow(12, 13))
        assertEquals(Decision.Allow, decide(at(12, 30), cfg))
        assertTrue(decide(at(11, 59), cfg) is Decision.Lock)
        assertTrue(decide(at(13), cfg) is Decision.Lock)
    }

    @Test
    fun `disabled break does not override a block`() {
        val cfg = config(block("day", 6, 20), breakWindow(12, 13, enabled = false))
        assertTrue(decide(at(12, 30), cfg) is Decision.Lock)
    }

    @Test
    fun `bedtime lock overrides a break`() {
        val cfg = config(breakWindow(21, 23))
        val decision = decide(at(22), cfg)
        assertEquals(Decision.Lock(LockReason.BEDTIME, null, LocalDateTime.of(2026, 11, 12, 5, 45)), decision)
    }

    @Test
    fun `bedtime lock wins over an active block`() {
        val decision = decide(at(23), config(block("late", 20, 23, id = 5)))
        assertEquals(LockReason.BEDTIME, (decision as Decision.Lock).reason)
    }

    @Test
    fun `bedtime starts at bedtime minus lead`() {
        val cfg = config()
        assertEquals(Decision.Allow, decide(at(21, 44), cfg))
        assertTrue(decide(at(21, 45), cfg) is Decision.Lock)
    }

    @Test
    fun `changing the lead moves the lock start without any rebuild`() {
        val thirtyMinuteLead = config(bedtime = bedtime.copy(leadMinutes = 30))
        assertEquals(Decision.Allow, decide(at(21, 59), thirtyMinuteLead))
        assertTrue(decide(at(22), thirtyMinuteLead) is Decision.Lock)
    }

    @Test
    fun `changing bedtime moves the lock start`() {
        val later = config(bedtime = bedtime.copy(bedtimeMinutes = 23 * 60))
        assertEquals(Decision.Allow, decide(at(22, 14), later))
        assertTrue(decide(at(22, 15), later) is Decision.Lock)
    }

    @Test
    fun `lead longer than bedtime wraps to the previous evening`() {
        val earlyBedtime = BedtimeRule(bedtimeMinutes = 30, leadMinutes = 60, endMinutes = 5 * 60, days = everyDay)
        assertEquals(23 * 60 + 30, earlyBedtime.startMinutes)
    }

    @Test
    fun `bedtime lock is still active after midnight`() {
        val decision = decide(at(2), config())
        assertEquals(LockReason.BEDTIME, (decision as Decision.Lock).reason)
    }

    @Test
    fun `overlapping blocks lock until the last one ends`() {
        val cfg = config(block("a", 6, 9, id = 1), block("b", 8, 11, id = 2))
        val decision = decide(at(8, 30), cfg) as Decision.Lock
        assertEquals(at(11), decision.endsAt)
        assertEquals("b", decision.ruleName)
    }

    @Test
    fun `block that starts later is not active yet`() {
        assertEquals(Decision.Allow, decide(at(5, 59), config(block("work", 6, 9))))
    }

    @Test
    fun `no product number is baked in - same engine with different settings gives different answers`() {
        val a = config(block("work", 6, 9))
        val b = config(block("work", 10, 12))
        assertTrue(decide(at(7), a) is Decision.Lock)
        assertEquals(Decision.Allow, decide(at(7), b))
    }
}
