package com.polysius.applocker.rules

import java.time.DayOfWeek
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/** One test per way of weakening a rule during a block or the bedtime lock (FR-40, FR-43). */
class StrictGuardTest {

    private val everyDay = DayOfWeek.entries.toSet()

    private fun rule(kind: ScheduleKind, from: Int, to: Int, enabled: Boolean = true) =
        ScheduleRule(1, "rule", kind, TimeWindow(everyDay, from * 60, to * 60), enabled)

    private val bedtime = BedtimeRule(22 * 60 + 30, 45, 5 * 60 + 45, everyDay)
    private fun config(vararg rules: ScheduleRule) = RulesConfig(rules.toList(), bedtime)

    private fun at(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, 11, hour, minute)

    private val allowed = StrictResult.Allowed

    // Editing, disabling and deleting an active rule

    @Test
    fun `active block cannot be edited disabled or deleted`() {
        val block = rule(ScheduleKind.BLOCK, 13, 16)
        assertEquals(StrictResult.Refused(at(16)), StrictGuard.changeRule(block, at(14)))
    }

    @Test
    fun `inactive block can be changed`() {
        val block = rule(ScheduleKind.BLOCK, 13, 16)
        assertEquals(allowed, StrictGuard.changeRule(block, at(12)))
    }

    @Test
    fun `active break cannot be shortened or deleted`() {
        val breakWindow = rule(ScheduleKind.BREAK, 12, 13)
        assertEquals(StrictResult.Refused(at(13)), StrictGuard.changeRule(breakWindow, at(12, 30)))
    }

    @Test
    fun `disabled rule that would be active is not treated as active`() {
        val block = rule(ScheduleKind.BLOCK, 13, 16, enabled = false)
        assertEquals(allowed, StrictGuard.changeRule(block, at(14)))
    }

    // Saving: creating or enabling a break that would open during the current lock

    @Test
    fun `break that is active during a block cannot be created`() {
        val cfg = config(rule(ScheduleKind.BLOCK, 10, 14))
        val breakWindow = rule(ScheduleKind.BREAK, 12, 13)
        assertEquals(StrictResult.Refused(at(14)), StrictGuard.saveRule(breakWindow, cfg, at(12, 30)))
    }

    @Test
    fun `break that opens a minute from now is refused during a block`() {
        val cfg = config(rule(ScheduleKind.BLOCK, 10, 12))
        val soon = rule(ScheduleKind.BREAK, 10, 13).copy(
            window = TimeWindow(everyDay, 10 * 60 + 31, 13 * 60),
        )
        assertEquals(StrictResult.Refused(at(12)), StrictGuard.saveRule(soon, cfg, at(10, 30)))
    }

    @Test
    fun `break that opens only after the lock ends is allowed`() {
        val cfg = config(rule(ScheduleKind.BLOCK, 10, 12))
        val lunch = rule(ScheduleKind.BREAK, 12, 13)
        assertEquals(allowed, StrictGuard.saveRule(lunch, cfg, at(10, 30)))
    }

    @Test
    fun `break during the bedtime lock is refused`() {
        val night = rule(ScheduleKind.BREAK, 22, 23)
        assertEquals(
            StrictResult.Refused(LocalDateTime.of(2026, 11, 12, 5, 45)),
            StrictGuard.saveRule(night, config(), at(22)),
        )
    }

    @Test
    fun `break can be created when no lock is in force`() {
        val cfg = config(rule(ScheduleKind.BLOCK, 6, 9))
        assertEquals(allowed, StrictGuard.saveRule(rule(ScheduleKind.BREAK, 12, 13), cfg, at(9)))
    }

    @Test
    fun `disabled break that would be active during a block can be saved`() {
        val cfg = config(rule(ScheduleKind.BLOCK, 10, 14))
        val breakWindow = rule(ScheduleKind.BREAK, 12, 13, enabled = false)
        assertEquals(allowed, StrictGuard.saveRule(breakWindow, cfg, at(12, 30)))
    }

    @Test
    fun `block that is active now can be created because it only adds locking`() {
        val block = rule(ScheduleKind.BLOCK, 12, 13)
        assertEquals(allowed, StrictGuard.saveRule(block, config(), at(12, 30)))
    }

    // All-apps mode

    private val lockedApps = setOf("com.example.insta")

    @Test
    fun `all apps mode cannot be switched on during a block if it would open a locked app`() {
        val cfg = config(rule(ScheduleKind.BLOCK, 13, 16))
        val result = StrictGuard.enableAllAppsMode(cfg, at(14), lockedApps, allowlistedPackages = lockedApps)
        assertEquals(StrictResult.Refused(at(16)), result)
    }

    @Test
    fun `all apps mode can be switched on during a block if no locked app is allowlisted`() {
        val cfg = config(rule(ScheduleKind.BLOCK, 13, 16))
        val result = StrictGuard.enableAllAppsMode(cfg, at(14), lockedApps, allowlistedPackages = setOf("com.example.maps"))
        assertEquals(allowed, result)
    }

    @Test
    fun `all apps mode can be switched on outside a block even with an overlapping allowlist`() {
        val cfg = config(rule(ScheduleKind.BLOCK, 13, 16))
        val result = StrictGuard.enableAllAppsMode(cfg, at(10), lockedApps, allowlistedPackages = lockedApps)
        assertEquals(allowed, result)
    }

    // Bedtime

    @Test
    fun `bedtime settings cannot change during the bedtime lock`() {
        assertEquals(
            StrictResult.Refused(LocalDateTime.of(2026, 11, 12, 5, 45)),
            StrictGuard.changeBedtime(config(), at(23)),
        )
    }

    @Test
    fun `bedtime whose lock would start exactly when it ends is not valid`() {
        // Bedtime 06:30 with a 45 minute lead starts the lock at 05:45, the same minute it ends.
        val bad = BedtimeRule(6 * 60 + 30, 45, 5 * 60 + 45, everyDay)
        assertEquals(false, bad.isValid)
        assertEquals(true, bedtime.isValid)
    }

    @Test
    fun `bedtime settings can change outside the bedtime lock`() {
        assertEquals(allowed, StrictGuard.changeBedtime(config(), at(15)))
    }

    // Unlocking an app, leaving all-apps mode, allowlist additions and turning protection off

    @Test
    fun `protection cannot be weakened during a block`() {
        val cfg = config(rule(ScheduleKind.BLOCK, 13, 16))
        assertEquals(StrictResult.Refused(at(16)), StrictGuard.weakenProtection(cfg, at(14)))
    }

    @Test
    fun `protection cannot be weakened during the bedtime lock`() {
        assertEquals(
            StrictResult.Refused(LocalDateTime.of(2026, 11, 12, 5, 45)),
            StrictGuard.weakenProtection(config(), at(23)),
        )
    }

    @Test
    fun `protection cannot be weakened during a block even if a break overlaps it`() {
        val cfg = config(rule(ScheduleKind.BLOCK, 6, 20), rule(ScheduleKind.BREAK, 12, 13))
        assertEquals(StrictResult.Refused(at(20)), StrictGuard.weakenProtection(cfg, at(12, 30)))
    }

    @Test
    fun `protection can be weakened when nothing is active`() {
        val cfg = config(rule(ScheduleKind.BLOCK, 13, 16))
        assertEquals(allowed, StrictGuard.weakenProtection(cfg, at(10)))
    }

    @Test
    fun `refusal names the end of the longest active rule`() {
        val cfg = RulesConfig(
            listOf(
                ScheduleRule(1, "a", ScheduleKind.BLOCK, TimeWindow(everyDay, 20 * 60, 22 * 60), true),
                ScheduleRule(2, "b", ScheduleKind.BLOCK, TimeWindow(everyDay, 21 * 60, 23 * 60), true),
            ),
            bedtime,
        )
        assertEquals(StrictResult.Refused(at(23)), StrictGuard.weakenProtection(cfg, at(21, 30)))
    }

    @Test
    fun `disabled block does not count as an active rule when weakening protection`() {
        val cfg = config(rule(ScheduleKind.BLOCK, 13, 16, enabled = false))
        assertEquals(allowed, StrictGuard.weakenProtection(cfg, at(14)))
    }

    @Test
    fun `active break alone does not stop protection being weakened`() {
        val cfg = config(rule(ScheduleKind.BREAK, 13, 16))
        assertEquals(allowed, StrictGuard.weakenProtection(cfg, at(14)))
    }
}
