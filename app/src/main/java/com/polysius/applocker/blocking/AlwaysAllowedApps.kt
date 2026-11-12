package com.polysius.applocker.blocking

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.Settings
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager

/**
 * Apps that are never blocked (FR-03): the dialler, the launcher, the alarm clock app, Settings and
 * AppLocker itself. They are looked up on the device, because the user may change the default
 * dialler or launcher.
 *
 * Only system apps and the user's chosen defaults count. An ordinary app that merely offers to
 * handle a dial or alarm intent is not always allowed.
 */
class AlwaysAllowedApps(private val context: Context) {

    private val resolved = TimedValue(CACHE_MILLIS, SystemClock::elapsedRealtime, ::lookUp)

    fun isAlwaysAllowed(packageName: String): Boolean = resolved.get().contains(packageName)

    /**
     * System UI and the keyboard are not "apps the user opened". Events from them must not count
     * as leaving or entering a locked app, so the service ignores them completely.
     */
    fun isIgnoredWindow(packageName: String): Boolean =
        packageName == SYSTEM_UI_PACKAGE || packageName == ANDROID_PACKAGE || packageName in inputMethodPackages()

    private fun lookUp(): Set<String> {
        val packages = mutableSetOf(context.packageName, SYSTEM_UI_PACKAGE)
        context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let(packages::add)

        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packages += defaultHandler(homeIntent)
        packages += systemAppsHandling(homeIntent)
        packages += systemAppsHandling(Intent(Intent.ACTION_DIAL))
        packages += systemAppsHandling(Intent(AlarmClock.ACTION_SHOW_ALARMS))
        packages += systemAppsHandling(Intent(Settings.ACTION_SETTINGS))
        return packages
    }

    /** The app the user has chosen for [intent], if there is one (not the system chooser). */
    private fun defaultHandler(intent: Intent): Set<String> {
        val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val packageName = resolved?.activityInfo?.packageName ?: return emptySet()
        return if (packageName == ANDROID_PACKAGE) emptySet() else setOf(packageName)
    }

    private fun systemAppsHandling(intent: Intent): Set<String> =
        context.packageManager.queryIntentActivities(intent, 0)
            .map { it.activityInfo.applicationInfo }
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 }
            .map { it.packageName }
            .toSet()

    private fun inputMethodPackages(): Set<String> =
        context.getSystemService(InputMethodManager::class.java)
            ?.enabledInputMethodList.orEmpty()
            .map { it.packageName }
            .toSet()

    private companion object {
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        const val ANDROID_PACKAGE = "android"

        /** How long the looked-up set is reused. Tuning constant, not a product setting. */
        const val CACHE_MILLIS = 60_000L
    }
}
