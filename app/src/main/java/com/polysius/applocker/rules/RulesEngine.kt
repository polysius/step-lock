package com.polysius.applocker.rules

import java.time.LocalDateTime

/**
 * Which apps the rules apply to, as seen at one moment. [restricted] answers "is this app in the
 * locked set?" so the engine does not care whether the user picked a list or all-apps mode.
 */
class AppScope(
    private val restricted: (String) -> Boolean,
    private val alwaysAllowed: (String) -> Boolean,
) {
    fun isRestricted(packageName: String): Boolean = restricted(packageName)
    fun isAlwaysAllowed(packageName: String): Boolean = alwaysAllowed(packageName)
}

/**
 * The decision for one foreground app (FR-21, Phase 1 subset). There is no earned balance yet, so
 * outside bedtime and blocks the app is allowed.
 *
 * Order: not locked, always allowed, bedtime, block without a break, otherwise allow.
 */
object RulesEngine {

    fun decide(
        packageName: String,
        now: LocalDateTime,
        config: RulesConfig,
        scope: AppScope,
    ): Decision {
        if (!scope.isRestricted(packageName)) return Decision.Allow
        if (scope.isAlwaysAllowed(packageName)) return Decision.Allow
        return currentLock(now, config)
    }

    /** The lock in force right now for a locked app, or [Decision.Allow] if there is none. */
    fun currentLock(now: LocalDateTime, config: RulesConfig): Decision {
        val bedtimeEnd = config.bedtime.window.activeUntil(now)
        if (bedtimeEnd != null) return Decision.Lock(LockReason.BEDTIME, null, bedtimeEnd)

        val enabledRules = config.rules.filter { it.isEnabled }
        val breakIsActive = enabledRules.any { it.kind == ScheduleKind.BREAK && it.window.isActive(now) }
        if (breakIsActive) return Decision.Allow

        // With overlapping blocks the lock lasts until the last one ends.
        return enabledRules
            .filter { it.kind == ScheduleKind.BLOCK }
            .mapNotNull { rule -> rule.window.activeUntil(now)?.let { end -> rule to end } }
            .maxByOrNull { (_, end) -> end }
            ?.let { (rule, end) -> Decision.Lock(LockReason.SCHEDULE, rule.name, end) }
            ?: Decision.Allow
    }
}
