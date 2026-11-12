package com.polysius.applocker.settings

/**
 * Initial values for the user-editable settings.
 *
 * These are presets only (spec section 3.4). They are written to storage once on first run by
 * [SettingsRepository.seedDefaultsIfNeeded]; after that, the stored value governs behaviour. The
 * only other use is [SettingsRepository] reading before seeding has finished. No rule or screen
 * may use these constants to make a decision.
 */
object AppDefaults {
    /** 22:30, expressed as minutes since local midnight. */
    const val DEFAULT_BEDTIME_MINUTES = 22 * 60 + 30

    /** The bedtime lock starts this many minutes before bedtime. */
    const val DEFAULT_BEDTIME_LEAD_MINUTES = 45

    /** The bedtime lock ends at 05:45 the next morning. */
    const val DEFAULT_BEDTIME_END_MINUTES = 5 * 60 + 45

    /** Lock a chosen list of apps, not every app. */
    const val DEFAULT_ALL_APPS_MODE = false

    /** The day resets at local midnight. */
    const val DEFAULT_DAY_BOUNDARY_MINUTES = 0

    /** Countdown warnings, in minutes before an unlock window ends. */
    val DEFAULT_WARNING_THRESHOLD_MINUTES = listOf(5, 1)
}
