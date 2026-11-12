package com.polysius.applocker.blocking

import com.polysius.applocker.rules.AppScope
import com.polysius.applocker.rules.RulesConfig

/** A snapshot of everything needed to decide whether a foreground app is locked. */
data class EnforcementState(
    val config: RulesConfig,
    val lockedPackages: Set<String>,
    val allowlistedPackages: Set<String>,
    val allAppsMode: Boolean,
    val protectionEnabled: Boolean,
) {
    /**
     * In list mode an app is locked when the user locked it. In all-apps mode every app is locked
     * unless the user allowlisted it. Always-allowed apps are handled separately by the engine.
     */
    fun scope(alwaysAllowed: (String) -> Boolean) = AppScope(
        restricted = { packageName ->
            if (allAppsMode) packageName !in allowlistedPackages else packageName in lockedPackages
        },
        alwaysAllowed = alwaysAllowed,
    )
}
