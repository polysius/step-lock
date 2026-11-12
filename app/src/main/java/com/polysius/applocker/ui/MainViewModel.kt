package com.polysius.applocker.ui

import android.app.Application
import android.content.Intent
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.polysius.applocker.R
import com.polysius.applocker.appContainer
import com.polysius.applocker.blocking.EnforcementState
import com.polysius.applocker.data.HistoryEntryEntity
import com.polysius.applocker.data.LockedAppEntity
import com.polysius.applocker.permissions.GrantState
import com.polysius.applocker.permissions.PermissionChecker
import com.polysius.applocker.permissions.PermissionKind
import com.polysius.applocker.permissions.needsAttention
import com.polysius.applocker.rules.ScheduleRule
import com.polysius.applocker.rules.StrictResult
import com.polysius.applocker.settings.AppSettings
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { LOADING, SETUP, MAIN }

data class MainUiState(
    val screen: Screen = Screen.LOADING,
    val grants: Map<PermissionKind, GrantState> = emptyMap(),
) {
    val anyPermissionNeedsAttention: Boolean
        get() = grants.values.any { it.needsAttention }
}

/** An app the user can lock, as found on the device. */
data class InstalledApp(val packageName: String, val label: String)

/** A one-off message for the snackbar. [argument] fills the %1$s in [res]. */
data class UiMessage(@StringRes val res: Int, val argument: String? = null)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = application.appContainer
    private val permissionChecker = PermissionChecker(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings?> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val enforcementState: StateFlow<EnforcementState?> = container.enforcementState

    val rules: StateFlow<List<ScheduleRule>> = container.schedule.rules
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val lockedApps: StateFlow<List<LockedAppEntity>> = container.lockedApps.apps
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val history: StateFlow<List<HistoryEntryEntity>> = container.history.recent(HISTORY_LIMIT)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    private var appWasLeft = false

    val healthPermissions: Set<String> get() = permissionChecker.healthPermissions

    fun isAlwaysAllowed(packageName: String) = container.alwaysAllowedApps.isAlwaysAllowed(packageName)

    init {
        viewModelScope.launch { _installedApps.value = loadInstalledApps() }
    }

    /**
     * Re-reads every permission. Called whenever the app returns to the foreground, so the checklist
     * updates after the user comes back from a Settings screen.
     */
    fun refreshPermissions() {
        viewModelScope.launch {
            val grants = permissionChecker.checkAll()
            val reopened = appWasLeft
            appWasLeft = false
            _uiState.update { current ->
                val screen = current.screen.orInitialChoice(grants)
                val needsSetup = grants.values.any { it.needsAttention }
                current.copy(
                    grants = grants,
                    screen = if (reopened && needsSetup) Screen.SETUP else screen,
                )
            }
        }
    }

    /**
     * Called when the app leaves the screen. Android keeps the process alive after Back or Home, so
     * the next open must prompt again for anything still missing (FR-48), not only after a restart.
     */
    fun onAppLeft() {
        appWasLeft = true
    }

    fun showSetup() = _uiState.update { it.copy(screen = Screen.SETUP) }

    fun showMain() = _uiState.update { it.copy(screen = Screen.MAIN) }

    /** Leaves setup. Onboarding counts as complete only when nothing is missing. */
    fun finishSetup() {
        viewModelScope.launch {
            val allGranted = !_uiState.value.anyPermissionNeedsAttention
            container.settings.setOnboardingCompleted(allGranted)
        }
        showMain()
    }

    // Changes to the locking rules. Each one is checked by the strict guard inside AppController.

    fun saveRule(rule: ScheduleRule) = change { container.controller.saveRule(rule) }

    fun deleteRule(rule: ScheduleRule) = change { container.controller.deleteRule(rule) }

    fun setBedtime(bedtimeMinutes: Int, leadMinutes: Int, endMinutes: Int) =
        change { container.controller.setBedtime(bedtimeMinutes, leadMinutes, endMinutes) }

    fun setLocked(app: InstalledApp, locked: Boolean) =
        change { container.controller.setLocked(app.packageName, app.label, locked) }

    fun setAllowlisted(app: InstalledApp, allowlisted: Boolean) =
        change { container.controller.setAllowlisted(app.packageName, app.label, allowlisted) }

    fun setAllAppsMode(enabled: Boolean) = change { container.controller.setAllAppsMode(enabled) }

    fun setProtectionEnabled(enabled: Boolean) = change { container.controller.setProtectionEnabled(enabled) }

    private fun change(action: suspend () -> StrictResult) {
        viewModelScope.launch {
            val result = action()
            if (result is StrictResult.Refused) {
                _messages.emit(UiMessage(R.string.strict_refused, formatEndTime(result.until)))
            }
        }
    }

    private fun formatEndTime(until: LocalDateTime) = timeFormat.format(until)

    private suspend fun loadInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val packageManager = getApplication<Application>().packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(launcherIntent, 0)
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(packageManager).toString()) }
            .filter { it.packageName != getApplication<Application>().packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /** First launch, and any later launch with a missing permission, opens on setup (FR-48). */
    private fun Screen.orInitialChoice(grants: Map<PermissionKind, GrantState>): Screen {
        if (this != Screen.LOADING) return this
        return if (grants.values.any { it.needsAttention }) Screen.SETUP else Screen.MAIN
    }

    private companion object {
        const val HISTORY_LIMIT = 200
    }
}
