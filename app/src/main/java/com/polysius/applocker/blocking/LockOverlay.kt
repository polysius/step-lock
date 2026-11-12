package com.polysius.applocker.blocking

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.polysius.applocker.ui.AppLockerTheme
import com.polysius.applocker.ui.LockScreen
import com.polysius.applocker.ui.LockScreenInfo

/**
 * The full-screen lock screen, drawn as an Accessibility overlay window on top of the blocked app.
 * That window type needs no "Draw over other apps" permission and no activity launch.
 */
class LockOverlay(
    private val service: AccessibilityService,
    private val onBackToHome: () -> Unit,
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var root: FrameLayout? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    /** What the overlay is showing now, or null when it is hidden. */
    var currentInfo: LockScreenInfo? = null
        private set

    /** Returns false if Android refused the window; the caller then falls back to going Home. */
    fun show(info: LockScreenInfo): Boolean {
        if (root != null) hide()

        val owner = OverlayLifecycleOwner().also { it.start() }
        val composeView = ComposeView(service).apply {
            setContent { AppLockerTheme { LockScreen(info, onBackToHome) } }
        }
        // Compose looks for the owners from the window's root view, so they go on the container.
        val container = BackKeyFrameLayout(service, onBackToHome).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            addView(composeView)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.OPAQUE,
        ).apply {
            // Cover the status and navigation bars too, so no strip of the blocked app shows.
            fitInsetsTypes = 0
        }

        try {
            windowManager.addView(container, params)
        } catch (error: RuntimeException) {
            owner.stop()
            return false
        }
        container.requestFocus()

        root = container
        lifecycleOwner = owner
        currentInfo = info
        return true
    }

    fun hide() {
        root?.let {
            try {
                windowManager.removeViewImmediate(it)
            } catch (error: IllegalArgumentException) {
                // The window was already removed by the system; nothing is left to clean up.
            }
        }
        lifecycleOwner?.stop()
        root = null
        lifecycleOwner = null
        currentInfo = null
    }
}

/** Sends the Back key to Home, so Back never lands the user inside the blocked app. */
private class BackKeyFrameLayout(
    service: AccessibilityService,
    private val onBack: () -> Unit,
) : FrameLayout(service) {

    init {
        isFocusableInTouchMode = true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_UP) onBack()
        return true
    }
}

/** Compose needs a lifecycle and a saved-state owner on the view tree; a service window has neither. */
private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun start() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
