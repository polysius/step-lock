package com.polysius.applocker.settings

import com.polysius.applocker.rules.ScheduleKind

/**
 * First-run preset for the schedule and the app lists (spec section 4). Like [AppDefaults], these
 * values are written to the database once and are editable afterwards; no rule reads them.
 *
 * Names are generic on purpose: this file is in a public repository.
 */
object SchedulePreset {

    /** The one app locked while Phase 1 is tested (TBD-16). */
    const val DEFAULT_LOCKED_PACKAGE = "com.instagram.android"
    const val DEFAULT_LOCKED_LABEL = "Instagram"

    /** Apps the owner wants open during blocks (TBD-09), beyond the always-allowed system apps. */
    val DEFAULT_ALLOWLISTED_APPS = listOf(
        "com.sec.android.app.shealth" to "Samsung Health",
        "com.google.android.apps.maps" to "Maps",
    )

    class PresetRule(
        val name: String,
        val kind: ScheduleKind,
        val startMinutes: Int,
        val endMinutes: Int,
    )

    /** Every day of the week (TBD-08). The gym slot stays locked until earning exists (Phase 2). */
    val DEFAULT_RULES = listOf(
        PresetRule("Morning block", ScheduleKind.BLOCK, hm(6, 0), hm(9, 0)),
        PresetRule("Morning break", ScheduleKind.BREAK, hm(9, 0), hm(10, 0)),
        PresetRule("Midday block", ScheduleKind.BLOCK, hm(10, 0), hm(12, 0)),
        PresetRule("Lunch break", ScheduleKind.BREAK, hm(12, 0), hm(13, 0)),
        PresetRule("Afternoon block", ScheduleKind.BLOCK, hm(13, 0), hm(16, 0)),
        PresetRule("Gym slot (locked until earning exists)", ScheduleKind.BLOCK, hm(16, 0), hm(18, 0)),
        PresetRule("Evening block", ScheduleKind.BLOCK, hm(18, 0), hm(20, 0)),
    )

    private fun hm(hour: Int, minute: Int) = hour * 60 + minute
}
