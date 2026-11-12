package com.polysius.applocker.rules

import java.time.DayOfWeek

enum class ScheduleKind { BLOCK, BREAK }

/** A user-made schedule block or break window. */
data class ScheduleRule(
    val id: Long,
    val name: String,
    val kind: ScheduleKind,
    val window: TimeWindow,
    val isEnabled: Boolean,
)

/**
 * The bedtime lock. It starts [leadMinutes] before [bedtimeMinutes] (wrapping past midnight) and
 * ends at [endMinutes] the next morning. All three values are user settings.
 */
data class BedtimeRule(
    val bedtimeMinutes: Int,
    val leadMinutes: Int,
    val endMinutes: Int,
    val days: Set<DayOfWeek>,
) {
    val startMinutes: Int
        get() = Math.floorMod(bedtimeMinutes - leadMinutes, MINUTES_PER_DAY)

    val window: TimeWindow
        get() = TimeWindow(days, startMinutes, endMinutes)

    /**
     * A lock that starts exactly when it ends would last all day and could never be edited
     * again, so that combination is not accepted.
     */
    val isValid: Boolean
        get() = startMinutes != endMinutes
}

data class RulesConfig(
    val rules: List<ScheduleRule>,
    val bedtime: BedtimeRule,
)

enum class LockReason { BEDTIME, SCHEDULE }

sealed interface Decision {
    data object Allow : Decision

    /** [ruleName] is null for the bedtime lock. [endsAt] is when the lock stops applying. */
    data class Lock(
        val reason: LockReason,
        val ruleName: String?,
        val endsAt: java.time.LocalDateTime,
    ) : Decision
}
