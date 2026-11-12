package com.polysius.applocker.blocking

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Phase 0 stub. It exists so the app appears under Settings > Accessibility and the setup checklist
 * can detect whether the user turned it on. Foreground detection and the lock overlay arrive in
 * Phase 1.
 */
class AppLockerAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
