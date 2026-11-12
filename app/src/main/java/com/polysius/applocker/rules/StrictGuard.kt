package com.polysius.applocker.rules

import java.time.LocalDateTime

sealed interface StrictResult {
    data object Allowed : StrictResult

    /** The change is refused until [until]; the UI shows "Strict: cannot be changed until ...". */
    data class Refused(val until: LocalDateTime) : StrictResult
}

/**
 * Strict-only enforcement (FR-40). While a rule is active nothing may weaken it, and no change
 * may be a way to open a locked app. There is no override (FR-43), so this object has no
 * "force" parameter and no exception list.
 *
 * Changes that make the rules stricter, such as adding a block or locking another app, are
 * always allowed.
 */
object StrictGuard {

    /** Editing, disabling or deleting [rule] as it is saved today. */
    fun changeRule(rule: ScheduleRule, now: LocalDateTime): StrictResult {
        if (!rule.isEnabled) return StrictResult.Allowed
        return refuseIfActive(rule.window.activeUntil(now))
    }

    /**
     * Saving [rule] (new or edited). A break that opens at any moment before the current lock ends
     * is an unlock path, so it is refused, even if it starts a minute from now. A block that is
     * active right now only adds locking, so it is allowed.
     */
    fun saveRule(rule: ScheduleRule, config: RulesConfig, now: LocalDateTime): StrictResult {
        if (rule.kind != ScheduleKind.BREAK || !rule.isEnabled) return StrictResult.Allowed
        val lockEnd = activeLockEnd(config, now) ?: return StrictResult.Allowed

        val opensDuringLock = generateSequence(now) { it.plusMinutes(1) }
            .takeWhile { it < lockEnd }
            .any { rule.window.isActive(it) }
        return if (opensDuringLock) StrictResult.Refused(lockEnd) else StrictResult.Allowed
    }

    /** Changing bedtime time, lead or end time during the bedtime lock. */
    fun changeBedtime(config: RulesConfig, now: LocalDateTime): StrictResult =
        refuseIfActive(config.bedtime.window.activeUntil(now))

    /**
     * Anything that could unlock an app: unlocking an app, leaving all-apps mode, turning
     * protection off. Refused while a bedtime lock or a block is in force.
     */
    fun weakenProtection(config: RulesConfig, now: LocalDateTime): StrictResult =
        refuseIfActive(activeLockEnd(config, now))

    /**
     * Turning all-apps mode on is stricter, except that an app the user allowed in an earlier
     * all-apps session would open even though it is locked in list mode. That app is an unlock
     * path, so the switch is refused while a lock is in force and such an app exists.
     */
    fun enableAllAppsMode(
        config: RulesConfig,
        now: LocalDateTime,
        lockedPackages: Set<String>,
        allowlistedPackages: Set<String>,
    ): StrictResult {
        val wouldUnlockAnApp = lockedPackages.any { it in allowlistedPackages }
        return if (wouldUnlockAnApp) weakenProtection(config, now) else StrictResult.Allowed
    }

    /** When the lock now in force ends (the latest end of any active block or bedtime lock), or null. */
    private fun activeLockEnd(config: RulesConfig, now: LocalDateTime): LocalDateTime? {
        val bedtimeEnd = config.bedtime.window.activeUntil(now)
        val blockEnds = config.rules
            .filter { it.isEnabled && it.kind == ScheduleKind.BLOCK }
            .mapNotNull { it.window.activeUntil(now) }
        return (blockEnds + listOfNotNull(bedtimeEnd)).maxOrNull()
    }

    private fun refuseIfActive(activeUntil: LocalDateTime?): StrictResult =
        if (activeUntil == null) StrictResult.Allowed else StrictResult.Refused(activeUntil)
}
