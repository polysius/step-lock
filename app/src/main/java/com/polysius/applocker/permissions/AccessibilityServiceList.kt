package com.polysius.applocker.permissions

/**
 * Reads the system's list of enabled accessibility services (the value of
 * `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`, entries separated by colons).
 *
 * Kept free of Android classes so it can be unit tested on the JVM.
 */
object AccessibilityServiceList {

    /**
     * Android may store a service as `pkg/pkg.Class` or as the short form `pkg/.Class`, so the
     * caller passes every spelling it accepts.
     */
    fun isListed(enabledServices: String?, acceptedSpellings: Collection<String>): Boolean {
        if (enabledServices.isNullOrBlank()) return false
        return enabledServices
            .split(SEPARATOR)
            .any { entry -> acceptedSpellings.any { it.equals(entry.trim(), ignoreCase = true) } }
    }

    private const val SEPARATOR = ':'
}
