package com.polysius.applocker.blocking

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.polysius.applocker.HealthPermissionsRationaleActivity
import com.polysius.applocker.MainActivity
import com.polysius.applocker.R
import com.polysius.applocker.appContainer
import com.polysius.applocker.data.HistoryType
import com.polysius.applocker.rules.Decision
import com.polysius.applocker.rules.LockReason
import com.polysius.applocker.rules.RulesEngine
import com.polysius.applocker.ui.LockScreenInfo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Detects which app comes to the foreground and covers locked apps with the lock screen.
 *
 * It reads only the package name and window class of window-state events, never text or window
 * content (spec section 11).
 */
class AppLockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    private val appLockerActivityNames = setOf(
        MainActivity::class.java.name,
        HealthPermissionsRationaleActivity::class.java.name,
    )

    private lateinit var overlay: LockOverlay

    /** The last app the user was in, kept so a block that starts while it is open still locks it. */
    private var foregroundPackage: String? = null

    private var recheckJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Android can connect the service again; start from a clean overlay and a single loop.
        if (::overlay.isInitialized) overlay.hide()
        recheckJob?.cancel()
        overlay = LockOverlay(this, ::goHome)

        recheckJob = serviceScope.launch {
            // Settings, schedule and app list changes apply to the app that is open right now.
            launch {
                appContainer.enforcementState.collect {
                    foregroundPackage?.let { evaluate(it, eventUptime = null) }
                }
            }
            while (true) {
                delay(RECHECK_INTERVAL_MILLIS)
                foregroundPackage?.let { evaluate(it, eventUptime = null) }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        val container = appContainer

        if (container.alwaysAllowedApps.isIgnoredWindow(packageName)) return

        if (packageName == this.packageName) {
            // Our own overlay window also raises events. Only a real AppLocker screen clears it.
            val isAppLockerScreen = event.className?.toString() in appLockerActivityNames
            if (isAppLockerScreen) {
                foregroundPackage = packageName
                overlay.hide()
            }
            return
        }

        foregroundPackage = packageName
        evaluate(packageName, eventUptime = event.eventTime)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceScope.cancel()
        if (::overlay.isInitialized) overlay.hide()
        super.onDestroy()
    }

    private fun evaluate(packageName: String, eventUptime: Long?) {
        val container = appContainer
        val state = container.enforcementState.value
        if (state == null || !state.protectionEnabled) {
            overlay.hide()
            return
        }

        val decision = RulesEngine.decide(
            packageName = packageName,
            now = LocalDateTime.now(),
            config = state.config,
            scope = state.scope(container.alwaysAllowedApps::isAlwaysAllowed),
        )
        when (decision) {
            Decision.Allow -> overlay.hide()
            is Decision.Lock -> lock(packageName, decision, eventUptime)
        }
    }

    private fun lock(packageName: String, decision: Decision.Lock, eventUptime: Long?) {
        val info = buildLockScreenInfo(packageName, decision)
        // The same text for the same app means the lock screen is already right. New text, such as
        // one block ending as the next begins, replaces it.
        if (overlay.currentInfo == info) return

        if (!overlay.show(info)) {
            // Android refused the overlay window. Sending the user Home is the fallback.
            goHome()
            return
        }

        // Time from the window event to the lock screen being added, reported in History so the
        // owner can judge the flash (FR-05). Re-checks have no event, so they report none.
        val latencyMillis = eventUptime?.let { SystemClock.uptimeMillis() - it }
        val latencyText = latencyMillis?.let { ", lock screen shown after $it ms" }.orEmpty()
        val reason = if (decision.reason == LockReason.BEDTIME) "bedtime lock" else "block \"${decision.ruleName}\""
        serviceScope.launch(Dispatchers.IO) {
            appContainer.history.log(
                HistoryType.BLOCK_ATTEMPT,
                "Blocked by $reason$latencyText",
                packageName,
                System.currentTimeMillis(),
            )
        }
    }

    private fun buildLockScreenInfo(packageName: String, decision: Decision.Lock): LockScreenInfo {
        val endsAt = decision.endsAt.format(timeFormat)
        val headline = when (decision.reason) {
            LockReason.BEDTIME -> getString(R.string.lock_headline_bedtime)
            LockReason.SCHEDULE -> getString(R.string.lock_headline_schedule)
        }
        val detail = when (decision.reason) {
            LockReason.BEDTIME -> getString(R.string.lock_detail_bedtime, endsAt)
            LockReason.SCHEDULE -> getString(R.string.lock_detail_schedule, decision.ruleName, endsAt)
        }
        return LockScreenInfo(packageName, labelOf(packageName), headline, detail)
    }

    private fun labelOf(packageName: String): String =
        try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (error: PackageManager.NameNotFoundException) {
            packageName
        }

    private fun goHome() {
        // Forget the app so a re-check cannot cover the launcher while Home is still coming up.
        foregroundPackage = null
        overlay.hide()
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private companion object {
        /** How often a still-open app is re-checked, so a block that starts while it is open locks it. */
        const val RECHECK_INTERVAL_MILLIS = 15_000L
    }
}
